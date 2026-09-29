package com.chatfei.app

import okhttp3.*
import java.util.concurrent.TimeUnit

object RealtimeClient {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private val listeners = mutableSetOf<(String) -> Unit>()
    fun connect(token: String, apiBaseUrl: String) {
        if (socket != null) return
        val wsBase = apiBaseUrl.replaceFirst("http", "ws")
        val request = Request.Builder().url("$wsBase/ws?token=$token").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) = listeners.toList().forEach { it(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { socket = null }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { socket = null }
        })
    }
    fun listen(listener: (String) -> Unit) { listeners += listener }
    fun remove(listener: (String) -> Unit) { listeners -= listener }
    fun close() { socket?.close(1000, "service stopped"); socket = null }
}
