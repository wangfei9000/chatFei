package com.chatfei.app

data class Session(val userId: String, val nickname: String, val token: String)
data class OnlineUser(val userId: String, val nickname: String)
data class ChatMember(val id: String, val nickname: String)
data class Conversation(val id: String, val type: String, val name: String, val members: List<ChatMember>)
data class ChatMessage(val id: String, val clientMessageId: String, val conversationId: String, val senderId: String, val senderNickname: String, val type: String, val text: String?, val mediaId: String?, val createdAt: String)

