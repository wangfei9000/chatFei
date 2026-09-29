@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chatfei.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MaterialTheme { val vm:ChatViewModel=viewModel();ChatFeiApp(vm) } }
    }
}

@Composable fun ChatFeiApp(vm:ChatViewModel){
    val state by vm.state.collectAsStateWithLifecycle()
    if(state.active!=null){ChatScreen(state,vm);return}
    var tab by remember{mutableIntStateOf(0)}
    Scaffold(bottomBar={NavigationBar{
        NavigationBarItem(tab==0,{tab=0},{Icon(Icons.AutoMirrored.Filled.Chat,"聊天")},label={Text("聊天")})
        NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Default.People,"在线")},label={Text("在线")})
        NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Default.Person,"我的")},label={Text("我的")})
    }}){padding->Box(Modifier.padding(padding).fillMaxSize()){
        when{state.loading->CircularProgressIndicator(Modifier.align(Alignment.Center));tab==0->ConversationList(state.conversations,vm::open);tab==1->OnlineList(state.online,vm::startPrivate,vm::createGroup);else->Profile(state,vm::rename,vm::updateApiBaseUrl)}
        state.error?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.align(Alignment.BottomCenter).padding(16.dp))}
    }}
}

@Composable private fun ConversationList(values:List<Conversation>,open:(Conversation)->Unit){
    Column{TopAppBar(title={Text("聊天")});if(values.isEmpty())Empty("还没有聊天，去在线列表找个人吧")else LazyColumn{items(values,key={it.id}){c->ListItem(headlineContent={Text(c.name)},supportingContent={Text(if(c.type=="GROUP")"群聊 · ${c.members.size}人" else "私聊")},leadingContent={Icon(if(c.type=="GROUP")Icons.Default.Groups else Icons.Default.Person,null)},modifier=Modifier.fillMaxWidth(),trailingContent={IconButton({open(c)}){Icon(Icons.Default.ChevronRight,"打开")}});HorizontalDivider()}}}
}
@Composable private fun OnlineList(values:List<OnlineUser>,start:(OnlineUser)->Unit,createGroup:(String,List<String>)->Unit){
    var dialog by remember{mutableStateOf(false)}
    Column{TopAppBar(title={Text("在线的人")},actions={IconButton({dialog=true}){Icon(Icons.Default.GroupAdd,"创建群聊")}});if(values.isEmpty())Empty("暂时没有其他人在线")else LazyColumn{items(values,key={it.userId}){u->ListItem(headlineContent={Text(u.nickname)},supportingContent={Text("在线")},leadingContent={Icon(Icons.Default.Circle,null,tint=MaterialTheme.colorScheme.primary)},trailingContent={Button({start(u)}){Text("聊天")}});HorizontalDivider()}}}
    if(dialog){var name by remember{mutableStateOf("")};val selected=remember{mutableStateListOf<String>()};AlertDialog(onDismissRequest={dialog=false},title={Text("创建群聊")},text={Column{OutlinedTextField(name,{name=it},label={Text("群名称")});Spacer(Modifier.height(8.dp));values.forEach{u->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(selected.contains(u.userId),{if(it)selected.add(u.userId)else selected.remove(u.userId)});Text(u.nickname)}}}},confirmButton={TextButton({createGroup(name,selected.toList());dialog=false},enabled=selected.size>=2&&name.trim().length>=2){Text("创建")}},dismissButton={TextButton({dialog=false}){Text("取消")}})}
}
@Composable private fun Profile(state:AppState,rename:(String)->Unit,updateApiBaseUrl:(String)->Unit){
    var editingName by remember{mutableStateOf(false)};var name by remember(state.session?.nickname){mutableStateOf(state.session?.nickname.orEmpty())}
    var editingServer by remember{mutableStateOf(false)};var server by remember(state.apiBaseUrl){mutableStateOf(state.apiBaseUrl)}
    Column{TopAppBar(title={Text("我的")});ListItem(headlineContent={Text(state.session?.nickname.orEmpty())},supportingContent={Text("匿名用户")},trailingContent={TextButton({editingName=true}){Text("修改")}});HorizontalDivider();ListItem(headlineContent={Text("服务器地址")},supportingContent={Text(state.apiBaseUrl)},leadingContent={Icon(Icons.Default.Dns,null)},trailingContent={TextButton({server=state.apiBaseUrl;editingServer=true}){Text("修改")}})}
    if(editingName)AlertDialog(onDismissRequest={editingName=false},title={Text("修改昵称")},text={OutlinedTextField(name,{name=it},singleLine=true)},confirmButton={TextButton({rename(name);editingName=false}){Text("保存")}},dismissButton={TextButton({editingName=false}){Text("取消")}})
    if(editingServer)AlertDialog(onDismissRequest={editingServer=false},title={Text("修改服务器地址")},text={Column{OutlinedTextField(server,{server=it},label={Text("API Base URL")},singleLine=true);Spacer(Modifier.height(8.dp));Text("保存后将重新连接并创建此服务器上的会话",style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton({updateApiBaseUrl(server);editingServer=false}){Text("保存")}},dismissButton={TextButton({editingServer=false}){Text("取消")}})
}
@Composable private fun Empty(text:String)=Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}

@Composable private fun ChatScreen(state:AppState,vm:ChatViewModel){
    var text by remember{mutableStateOf("")}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let(vm::sendMedia)}
    Scaffold(topBar={TopAppBar(title={Text(state.active?.name.orEmpty())},navigationIcon={IconButton(vm::closeChat){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})},bottomBar={Row(Modifier.padding(8.dp).navigationBarsPadding(),verticalAlignment=Alignment.CenterVertically){IconButton({picker.launch(arrayOf("image/*","video/*"))}){Icon(Icons.Default.AttachFile,"发送图片或视频")};OutlinedTextField(text,{text=it},Modifier.weight(1f),placeholder={Text("输入消息")},maxLines=3);IconButton({vm.send(text);text=""}){Icon(Icons.AutoMirrored.Filled.Send,"发送")}}}){padding->LazyColumn(Modifier.padding(padding).fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(state.messages,key={it.id}){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.senderId==state.session?.userId)Arrangement.End else Arrangement.Start){Surface(color=if(m.senderId==state.session?.userId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.large){Column(Modifier.padding(10.dp).widthIn(max=280.dp)){if(m.senderId!=state.session?.userId)Text(m.senderNickname,style=MaterialTheme.typography.labelSmall);Text(when(m.type){"IMAGE"->"[图片]";"VIDEO"->"[视频]";else->m.text.orEmpty()})}}}}}}
}
