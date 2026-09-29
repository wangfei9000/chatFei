package com.chatfei.app

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ApiClient(private val baseUrl: String) {
    private fun connection(path: String, method: String, token: String? = null): HttpURLConnection =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 10_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    private fun body(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (c.responseCode !in 200..299) error(JSONObject(text.ifBlank { "{}" }).optString("error", "请求失败 ${c.responseCode}"))
        return text
    }
    fun createSession(): Session {
        val c = connection("/api/session", "POST"); c.doOutput = true; c.outputStream.use { }
        val j = JSONObject(body(c)); return Session(j.getString("userId"), j.getString("nickname"), j.getString("token"))
    }
    fun online(token: String): List<OnlineUser> = JSONArray(body(connection("/api/users/online", "GET", token))).objects().map { OnlineUser(it.getString("userId"), it.getString("nickname")) }
    fun conversations(token: String): List<Conversation> = JSONArray(body(connection("/api/conversations", "GET", token))).objects().map { it.conversation() }
    fun messages(token: String, id: String): List<ChatMessage> = JSONArray(body(connection("/api/conversations/$id/messages?limit=100", "GET", token))).objects().map { it.message() }.reversed()
    fun createPrivate(token: String, userId: String): Conversation = postJson("/api/conversations/private", token, JSONObject().put("userId", userId)).conversation()
    fun createGroup(token: String, name: String, ids: List<String>): Conversation = postJson("/api/conversations/groups", token, JSONObject().put("name", name).put("memberIds", JSONArray(ids))).conversation()
    fun sendText(token: String, conversationId: String, text: String): ChatMessage = postJson("/api/conversations/$conversationId/messages", token, JSONObject().put("clientMessageId", UUID.randomUUID().toString()).put("type", "TEXT").put("text", text)).message()
    fun rename(token: String, nickname: String): String = postJson("/api/users/me/nickname", token, JSONObject().put("nickname", nickname), "PUT").getString("nickname")
    fun upload(token: String, resolver: ContentResolver, uri: Uri, mime: String): String {
        val boundary = "ChatFei-${UUID.randomUUID()}"; val c = connection("/api/media", "POST", token)
        c.doOutput = true; c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary"); c.setChunkedStreamingMode(256 * 1024)
        c.outputStream.buffered().use { out ->
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"upload\"\r\nContent-Type: $mime\r\n\r\n".toByteArray())
            resolver.openInputStream(uri)!!.use { it.copyTo(out) }
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        return JSONObject(body(c)).getString("mediaId")
    }
    fun sendMedia(token: String, conversationId: String, mediaId: String, type: String): ChatMessage = postJson("/api/conversations/$conversationId/messages", token, JSONObject().put("clientMessageId", UUID.randomUUID().toString()).put("type", type).put("mediaId", mediaId)).message()
    private fun postJson(path: String, token: String, json: JSONObject, method: String = "POST"): JSONObject { val c=connection(path,method,token);c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(json.toString().toByteArray())};return JSONObject(body(c)) }
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.conversation(): Conversation = Conversation(getString("id"), getString("type"), getString("name"), getJSONArray("members").objects().map { ChatMember(it.getString("id"), it.getString("nickname")) })
private fun JSONObject.message(): ChatMessage = ChatMessage(getString("id"),getString("clientMessageId"),getString("conversationId"),getString("senderId"),getString("senderNickname"),getString("type"),nullableString("text"),nullableString("mediaId"),getString("createdAt"))
private fun JSONObject.nullableString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
