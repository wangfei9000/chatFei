package com.chatfei.app

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ApiClient(private val baseUrl: String, private val aesKey: String) {
    /** 创建带超时、JSON 响应类型和可选登录凭证的 HTTP 连接。 */
    private fun connection(path: String, method: String, token: String? = null): HttpURLConnection =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 10_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    /** 读取 HTTP 响应正文；非 2xx 状态会转换为包含服务端错误信息的异常。 */
    private fun body(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (c.responseCode !in 200..299) error(JSONObject(text.ifBlank { "{}" }).optString("error", "请求失败 ${c.responseCode}"))
        return text
    }
    /** 向服务器申请匿名聊天身份，并返回用户 ID、昵称和访问 token。 */
    fun createSession(): Session {
        val c = connection("/api/session", "POST"); c.doOutput = true; c.outputStream.use { }
        val j = JSONObject(body(c)); return Session(j.getString("userId"), j.getString("nickname"), j.getString("token"))
    }
    /** 查询当前通过 WebSocket 保持在线的用户列表。 */
    fun online(token: String): List<OnlineUser> = JSONArray(body(connection("/api/users/online", "GET", token))).objects().map { OnlineUser(it.getString("userId"), it.getString("nickname")) }
    /** 查询当前用户加入的全部私聊和群聊会话。 */
    fun conversations(token: String): List<Conversation> = JSONArray(body(connection("/api/conversations", "GET", token))).objects().map { it.conversation() }
    /** 拉取指定会话最近 100 条消息、解析并按时间正序返回。 */
    fun messages(token: String, id: String): List<ChatMessage> = JSONArray(body(connection("/api/conversations/$id/messages?limit=100", "GET", token))).objects().map { it.message(aesKey) }.reversed()
    /** 与指定用户创建私聊；已有私聊时服务器返回原会话。 */
    fun createPrivate(token: String, userId: String): Conversation = postJson("/api/conversations/private", token, JSONObject().put("userId", userId)).conversation()
    /** 使用群名称和成员 ID 列表创建群聊。 */
    fun createGroup(token: String, name: String, ids: List<String>): Conversation = postJson("/api/conversations/groups", token, JSONObject().put("name", name).put("memberIds", JSONArray(ids))).conversation()
    /** 加密文本、生成客户端消息 ID，并通过 HTTP 将文本消息发送到服务器。 */
    fun sendText(token: String, conversationId: String, text: String): ChatMessage = postJson("/api/conversations/$conversationId/messages", token, JSONObject().put("clientMessageId", UUID.randomUUID().toString()).put("type", "TEXT").put("encryptedText", AesTextCrypto.encrypt(text, aesKey))).message(aesKey)
    /** 修改当前用户昵称并返回服务器确认后的新昵称。 */
    fun rename(token: String, nickname: String): String = postJson("/api/users/me/nickname", token, JSONObject().put("nickname", nickname), "PUT").getString("nickname")
    /** 以 multipart/form-data 流式上传图片或视频，并返回服务器生成的媒体 ID。 */
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
    /** 把已上传媒体的 ID 作为图片或视频消息发送到指定会话。 */
    fun sendMedia(token: String, conversationId: String, mediaId: String, type: String): ChatMessage = postJson("/api/conversations/$conversationId/messages", token, JSONObject().put("clientMessageId", UUID.randomUUID().toString()).put("type", type).put("mediaId", mediaId)).message(aesKey)
    /** 发送 JSON 请求并将成功响应解析为 JSONObject。 */
    private fun postJson(path: String, token: String, json: JSONObject, method: String = "POST"): JSONObject { val c=connection(path,method,token);c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(json.toString().toByteArray())};return JSONObject(body(c)) }
}

/** 将 JSON 数组转换为可使用 Kotlin 集合操作的 JSONObject 列表。 */
private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
/** 将会话 JSON 转换为客户端 Conversation 数据模型。 */
private fun JSONObject.conversation(): Conversation = Conversation(getString("id"), getString("type"), getString("name"), getJSONArray("members").objects().map { ChatMember(it.getString("id"), it.getString("nickname")) })
/** 将消息 JSON 转换为客户端模型，并优先解密 encryptedText 字段。 */
private fun JSONObject.message(aesKey:String): ChatMessage { val encrypted=nullableString("encryptedText");val legacy=nullableString("text");val text=if(encrypted!=null)runCatching{AesTextCrypto.decrypt(encrypted,aesKey)}.getOrElse{"[无法解密：请检查 AES 密钥]"}else legacy;return ChatMessage(getString("id"),getString("clientMessageId"),getString("conversationId"),getString("senderId"),getString("senderNickname"),getString("type"),text,encrypted,nullableString("mediaId"),getString("createdAt")) }
/** 安全读取可缺省或值为 JSON null 的字符串字段。 */
private fun JSONObject.nullableString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
