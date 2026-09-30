package com.chatfei.app

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesTextCrypto {
    private const val IV_SIZE = 12
    fun validateKey(value: String): Boolean = runCatching { Base64.decode(value.trim(), Base64.DEFAULT).size == 32 }.getOrDefault(false)
    fun encrypt(plainText: String, keyBase64: String): String {
        val iv = ByteArray(IV_SIZE).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(keyBase64), GCMParameterSpec(128, iv))
        return Base64.encodeToString(iv + cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }
    fun decrypt(value: String, keyBase64: String): String {
        val payload = Base64.decode(value, Base64.DEFAULT)
        require(payload.size > IV_SIZE) { "无效的加密消息" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(keyBase64), GCMParameterSpec(128, payload.copyOfRange(0, IV_SIZE)))
        return String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)), StandardCharsets.UTF_8)
    }
    private fun key(value: String) = SecretKeySpec(Base64.decode(value.trim(), Base64.DEFAULT), "AES")
}
