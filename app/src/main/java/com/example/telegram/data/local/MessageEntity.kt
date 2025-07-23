package com.example.telegram.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val content: String,
    val isSentByMe: Boolean,
    val timestamp: Long,
    val senderId: String,
    val voiceUrl: String? = null
)
