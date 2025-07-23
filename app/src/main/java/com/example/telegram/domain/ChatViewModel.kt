package com.example.telegram.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegram.data.local.Chat
import com.example.telegram.data.local.Message
import com.example.telegram.data.local.MessageEntity
import com.example.telegram.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val _chatId = MutableStateFlow<String?>(null)

    private val _userEmails = MutableStateFlow<Map<String, String>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = _chatId
        .filterNotNull()
        .flatMapLatest { chatId ->
            repository.getMessages(chatId)
                .map { entities ->
                    val showSenderName = repository.isGroupChat(chatId)

                    entities.map { entity ->
                        val email = if (showSenderName && entity.senderId.isNotBlank()) {
                            _userEmails.value[entity.senderId] ?: "Unknown"
                        } else null

                        Message(
                            content = entity.content,
                            isSentByMe = entity.isSentByMe,
                            timestamp = entity.timestamp,
                            senderId = entity.senderId,
                            senderName = email,
                            voiceUrl = entity.voiceUrl
                        )
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    init {
        if (currentUserId != null) {
            loadUserChats()
        }
    }

    private fun loadUserChats() {
        viewModelScope.launch {
            repository.getChatsForUser(currentUserId!!).collect { chatList ->
                _chats.value = chatList
            }
        }
    }

    fun selectChat(chatId: String) {
        _chatId.value = chatId
        repository.listenForMessages(chatId)

        viewModelScope.launch {
            if (repository.isGroupChat(chatId)) {
                val entities = repository.getMessages(chatId).first()
                val senderIds = entities.map { it.senderId }.distinct().filter { it.isNotBlank() }

                val emails = mutableMapOf<String, String>()
                for (id in senderIds) {
                    repository.getUserEmail(id)?.let { email ->
                        emails[id] = email
                    }
                }
                _userEmails.value = emails
            } else {
                _userEmails.value = emptyMap()
            }
        }
    }


    fun sendMessage(content: String) {
        val chatId = _chatId.value
        if (chatId != null) {
            viewModelScope.launch {
                repository.sendMessage(chatId, content)
            }
        }
    }

    fun sendVoiceMessage(audioFilePath: String) {
        val chatId = _chatId.value
        if (chatId != null) {
            viewModelScope.launch {
                repository.sendVoiceMessage(chatId, audioFilePath)
            }
        }
    }

    fun deleteMessage(entity: MessageEntity) {
        viewModelScope.launch {
            repository.deleteMessage(entity)
        }
    }


}



