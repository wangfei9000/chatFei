package com.chatfei.app

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("identity", Context.MODE_PRIVATE)
    fun load(): Session? {
        val token = prefs.getString("token", null) ?: return null
        return Session(prefs.getString("userId", "")!!, prefs.getString("nickname", "")!!, token)
    }
    fun save(value: Session) = prefs.edit().putString("userId", value.userId).putString("nickname", value.nickname).putString("token", value.token).apply()
    fun rename(value: String) = prefs.edit().putString("nickname", value).apply()
    fun clear() = prefs.edit().clear().apply()
}

class AppConfigStore(context: Context) {
    companion object {
        const val DEFAULT_API_BASE_URL = "http://121.40.244.102:8082"
        const val DEFAULT_AES_KEY = "Q2hhdEZlaS1BRVMtMjU2LUtleS0yMDI2LTAwMDAwMDE="
    }
    private val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    fun apiBaseUrl(): String = prefs.getString("api_base_url", DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    fun saveApiBaseUrl(value: String) = prefs.edit().putString("api_base_url", value).apply()
    fun aesKey(): String = prefs.getString("aes_key", DEFAULT_AES_KEY) ?: DEFAULT_AES_KEY
    fun saveAesKey(value: String) = prefs.edit().putString("aes_key", value).apply()
}
