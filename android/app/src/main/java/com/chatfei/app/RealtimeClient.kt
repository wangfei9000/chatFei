package com.chatfei.app

import okhttp3.*
import java.util.concurrent.TimeUnit

object RealtimeClient {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private val listeners = mutableSetOf<(String) -> Unit>()
    /** 使用登录 token 连接服务器 WebSocket；已有连接时不重复创建。 */
    fun connect(token: String, apiBaseUrl: String) {
        if (socket != null) return
        val wsBase = apiBaseUrl.replaceFirst("http", "ws")
        val request = Request.Builder().url("$wsBase/ws?token=$token").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            /** 将服务器发来的文本事件分发给当前注册的所有页面监听器。 */
            override fun onMessage(webSocket: WebSocket, text: String) = listeners.toList().forEach { it(text) }
            /** 连接发生异常时清空引用，使前台服务下一轮检查能够重新连接。 */
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { socket = null }
            /** 连接正常关闭时清空引用，使后续连接请求可以重新建立连接。 */
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { socket = null }
        })
    }
    /** 注册实时事件监听器，通常由 ViewModel 用它触发消息列表刷新。 */
    fun listen(listener: (String) -> Unit) { listeners += listener }
    /** 移除不再使用的实时事件监听器，避免页面销毁后继续收到回调。 */
    fun remove(listener: (String) -> Unit) { listeners -= listener }
    /** 主动关闭 WebSocket 并释放连接引用，一般在前台服务停止时调用。 */
    fun close() { socket?.close(1000, "service stopped"); socket = null }
}
