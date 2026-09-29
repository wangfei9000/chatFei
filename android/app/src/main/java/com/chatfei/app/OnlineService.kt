package com.chatfei.app

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper

class OnlineService : Service() {
    companion object { private const val CHANNEL = "chatfei_online"; private const val NOTIFICATION = 1001 }
    private val handler = Handler(Looper.getMainLooper())
    private val reconnect = object : Runnable {
        override fun run() { SessionStore(this@OnlineService).load()?.let { RealtimeClient.connect(it.token, AppConfigStore(this@OnlineService).apiBaseUrl()) }; handler.postDelayed(this, 10_000) }
    }
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "在线状态", NotificationManager.IMPORTANCE_LOW))
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL).setContentTitle("ChatFei 在线中").setContentText("正在接收私聊和群聊消息").setSmallIcon(android.R.drawable.stat_notify_chat).setContentIntent(pending).setOngoing(true).build()
        startForeground(NOTIFICATION, notification)
        reconnect.run()
    }
    override fun onDestroy() { handler.removeCallbacks(reconnect); RealtimeClient.close(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
