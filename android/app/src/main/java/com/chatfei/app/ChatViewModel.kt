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
    val sendingMedia: Boolean = false,
    val apiBaseUrl: String = AppConfigStore.DEFAULT_API_BASE_URL,
    val aesKey: String = AppConfigStore.DEFAULT_AES_KEY
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app); private val config = AppConfigStore(app)
    private var api = ApiClient(config.apiBaseUrl(),config.aesKey())
    private val _state = MutableStateFlow(AppState(apiBaseUrl = config.apiBaseUrl(),aesKey=config.aesKey())); val state = _state.asStateFlow()
    private val realtimeListener: (String) -> Unit = { refresh() }
    init { RealtimeClient.listen(realtimeListener); bootstrap() }
    /** 恢复本地会话或创建新会话，然后启动在线服务并加载初始数据。 */
    private fun bootstrap() = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { store.load() ?: api.createSession().also(store::save) } }
            .onSuccess { session ->
                _state.value = _state.value.copy(session = session, loading = false)
                getApplication<Application>().startForegroundService(Intent(getApplication(), OnlineService::class.java)); refresh()
            }.onFailure(::fail)
    }
    /** 刷新会话和在线用户；若聊天页已打开，同时重新拉取该会话消息。 */
    fun refresh() { val s=_state.value.session?:return;viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { api.conversations(s.token) to api.online(s.token) } }.onSuccess { _state.value=_state.value.copy(conversations=it.first,online=it.second,error=null);_state.value.active?.let{open(it)} }.onFailure(::fail) } }
    /** 打开指定会话并从服务器加载其最近消息。 */
    fun open(c: Conversation) { val s=_state.value.session?:return;_state.value=_state.value.copy(active=c);viewModelScope.launch { runCatching { withContext(Dispatchers.IO){api.messages(s.token,c.id)} }.onSuccess{_state.value=_state.value.copy(messages=it)}.onFailure(::fail) } }
    /** 退出当前聊天页并清空内存中的消息列表。 */
    fun closeChat(){_state.value=_state.value.copy(active=null,messages=emptyList())}
    /** 与在线用户创建或恢复私聊，然后打开对应会话。 */
    fun startPrivate(user: OnlineUser){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.createPrivate(s.token,user.userId)}}.onSuccess{refresh();open(it)}.onFailure(::fail)}}
    /** 使用名称和成员 ID 创建群聊，然后刷新并打开新会话。 */
    fun createGroup(name:String,ids:List<String>){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.createGroup(s.token,name,ids)}}.onSuccess{refresh();open(it)}.onFailure(::fail)}}
    /** 发送非空文本消息；服务器确认成功后重新加载当前会话。 */
    fun send(text:String){val s=_state.value.session?:return;val c=_state.value.active?:return;if(text.isBlank())return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.sendText(s.token,c.id,text)}}.onSuccess{open(c)}.onFailure(::fail)}}
    /** 校验媒体类型、上传文件并将其作为图片或视频消息发送。 */
    fun sendMedia(uri: Uri){
        val s=_state.value.session?:return;val c=_state.value.active?:return
        val resolver=getApplication<Application>().contentResolver
        val mime=resolver.getType(uri)
        if(mime==null||(!mime.startsWith("image/")&&!mime.startsWith("video/"))){fail(IllegalArgumentException("无法识别所选图片或视频"));return}
        _state.value=_state.value.copy(sendingMedia=true,error=null)
        viewModelScope.launch{runCatching{withContext(Dispatchers.IO){val id=api.upload(s.token,resolver,uri,mime);api.sendMedia(s.token,c.id,id,if(mime.startsWith("image/"))"IMAGE" else "VIDEO")}}.onSuccess{_state.value=_state.value.copy(sendingMedia=false);open(c)}.onFailure{_state.value=_state.value.copy(sendingMedia=false);fail(it)}}
    }
    /** 修改昵称，持久化服务器返回的新值并刷新页面数据。 */
    fun rename(name:String){val s=_state.value.session?:return;viewModelScope.launch{runCatching{withContext(Dispatchers.IO){api.rename(s.token,name)}}.onSuccess{store.rename(it);_state.value=_state.value.copy(session=s.copy(nickname=it));refresh()}.onFailure(::fail)}}
    /** 校验并保存新的服务器地址，关闭旧连接后重新创建会话。 */
    fun updateApiBaseUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        if (!normalized.matches(Regex("https?://[^/\\s]+"))) { fail(IllegalArgumentException("请输入有效的 http:// 或 https:// 地址")); return }
        if (normalized == _state.value.apiBaseUrl) return
        config.saveApiBaseUrl(normalized); store.clear(); api = ApiClient(normalized,config.aesKey())
        getApplication<Application>().stopService(Intent(getApplication(), OnlineService::class.java)); RealtimeClient.close()
        _state.value = AppState(apiBaseUrl = normalized,aesKey=config.aesKey())
        bootstrap()
    }
    /** 校验并保存 AES 密钥，重建 API 客户端并重新解密当前会话消息。 */
    fun updateAesKey(value:String){val normalized=value.trim();if(!AesTextCrypto.validateKey(normalized)){fail(IllegalArgumentException("AES 密钥必须是 Base64 编码的 32 字节密钥"));return};config.saveAesKey(normalized);api=ApiClient(config.apiBaseUrl(),normalized);_state.value=_state.value.copy(aesKey=normalized,error=null);_state.value.active?.let(::open)}
    /** 将异步操作失败信息写入页面状态并结束加载状态。 */
    private fun fail(t:Throwable){_state.value=_state.value.copy(loading=false,error=t.message?:"请求失败")}
    /** ViewModel 销毁时注销 WebSocket 事件监听器，避免内存泄漏。 */
    override fun onCleared(){RealtimeClient.remove(realtimeListener);super.onCleared()}
}
