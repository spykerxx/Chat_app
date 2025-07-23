package com.example.telegram.data.local

data class Group(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val members: List<String> = emptyList() // real member IDs, not count
)
