package com.chatfei.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppState(
    val loading: Boolean = true,
    val session: Session? = null,
    val conversations: List<Conversation> = emptyList(),
    val online: List<OnlineUser> = emptyList(),
    val active: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val apiBaseUrl: String = AppConfigStore.DEFAULT_API_BASE_URL
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app); private val config = AppConfigStore(app)
    private var api = ApiClient(config.apiBaseUrl())
    private val _state = MutableStateFlow(AppState(apiBaseUrl = config.apiBaseUrl())); val state = _state.asStateFlow()
    private val realtimeListener: (String) -> Unit = { refresh() }
    init { RealtimeClient.listen(realtimeListener); bootstrap() }
    private fun bootstrap() = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { store.load() ?: api.createSession().also(store::save) } }
            .onSuccess { session ->
                _state.value = _state.value.copy(session = session, loading = false)
                getApplication<Application>().startForegroundService(Intent(getApplication(), OnlineService::class.java)); refresh()
            }.onFailure(::fail)
    }
    fun refresh() { val s=_state.value.session?:return;viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { api.conversations(s.token) to api.online(s.token) } }.onSuccess { _state.value=_state.value.copy(conversations=it.first,online=it.second,error=null);_state.value.active?.let{open(it)} }.onFailure(::fail) } }
    fun open(c: Conversation) { val s=_state.value.session?:return;_state.value=_state.value.copy(active=c);viewModelScope.launch { runCatching { withContext(Dispatchers.IO){api.messages(s.token,c.id)} }.onSuccess{_state.value=_state.value.copy(messages=it)}.onFailure(::fail) } }
    fun closeChat(){_state.value=_state.value.copy(active=null,messages=emptyList())}
    fun startPrivate(user: OnlineUser){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.createPrivate(s.token,user.userId)}}.onSuccess{refresh();open(it)}.onFailure(::fail)}}
    fun createGroup(name:String,ids:List<String>){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.createGroup(s.token,name,ids)}}.onSuccess{refresh();open(it)}.onFailure(::fail)}}
    fun send(text:String){val s=_state.value.session?:return;val c=_state.value.active?:return;if(text.isBlank())return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.sendText(s.token,c.id,text)}}.onSuccess{open(c)}.onFailure(::fail)}}
    fun sendMedia(uri: Uri){val s=_state.value.session?:return;val c=_state.value.active?:return;val resolver=getApplication<Application>().contentResolver;val mime=resolver.getType(uri)?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){val id=api.upload(s.token,resolver,uri,mime);api.sendMedia(s.token,c.id,id,if(mime.startsWith("image/"))"IMAGE" else "VIDEO")}}.onSuccess{open(c)}.onFailure(::fail)}}
    fun rename(name:String){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.rename(s.token,name)}}.onSuccess{store.rename(it);_state.value=_state.value.copy(session=s.copy(nickname=it));refresh()}.onFailure(::fail)}}
    fun updateApiBaseUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        if (!normalized.matches(Regex("https?://[^/\\s]+"))) { fail(IllegalArgumentException("请输入有效的 http:// 或 https:// 地址")); return }
        if (normalized == _state.value.apiBaseUrl) return
        config.saveApiBaseUrl(normalized); store.clear(); api = ApiClient(normalized)
        getApplication<Application>().stopService(Intent(getApplication(), OnlineService::class.java)); RealtimeClient.close()
        _state.value = AppState(apiBaseUrl = normalized)
        bootstrap()
    }
    private fun fail(t:Throwable){_state.value=_state.value.copy(loading=false,error=t.message?:"请求失败")}
    override fun onCleared(){RealtimeClient.remove(realtimeListener);super.onCleared()}
}
