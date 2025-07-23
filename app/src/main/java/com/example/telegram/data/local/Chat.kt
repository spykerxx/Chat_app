package com.example.telegram.data.local

data class Chat(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val time: String = "",
    val unreadCount: Int = 0,
    val members: List<String> = emptyList()
)