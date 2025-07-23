package com.example.telegram.data.local

data class Message(
    val content: String,
    val isSentByMe: Boolean,
    val timestamp: Long,
    val senderId: String = "",
    val senderName: String? = null,
    val voiceUrl: String? = null
)
