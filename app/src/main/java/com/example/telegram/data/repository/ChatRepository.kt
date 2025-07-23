package com.example.telegram.data.repository

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.example.telegram.data.local.Chat
import com.example.telegram.data.local.MessageDao
import com.example.telegram.data.local.MessageEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class ChatRepository(private val messageDao: MessageDao) {

    private var messagesListenerRegistration: ListenerRegistration? = null
    private var chatsListenerRegistration: ListenerRegistration? = null
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sendMessage(chatId: String, content: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val chatDocRef = firestore.collection("chats").document(chatId)

        val chatSnapshot = chatDocRef.get().await()
        if (!chatSnapshot.exists()) {
            chatDocRef.set(
                mapOf(
                    "name" to "Unknown Chat",
                    "lastMessage" to "",
                    "lastMessageTimestamp" to 0L,
                    "members" to listOf<String>()
                )
            ).await()
        }

        val messageDocRef = chatDocRef.collection("messages").document()
        val messageId = messageDocRef.id

        val message = hashMapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "content" to content,
            "senderId" to currentUserId,
            "timestamp" to System.currentTimeMillis()
        )

        messageDocRef.set(message).await()

        chatDocRef.update(
            mapOf(
                "lastMessage" to content,
                "lastMessageTimestamp" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun sendVoiceMessage(chatId: String, audioFilePath: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val chatDocRef = firestore.collection("chats").document(chatId)
        val messageDocRef = chatDocRef.collection("messages").document()
        val messageId = messageDocRef.id

        val file = File(audioFilePath)
        if (!file.exists()) {
            Log.e("ChatRepository", "Audio file does not exist at path: $audioFilePath")
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference.child("voiceMessages/$chatId/$messageId.m4a")

        try {
            val fileUri = Uri.fromFile(file)
            Log.d("ChatRepository", "Uploading audio file at path: $audioFilePath")
            Log.d("ChatRepository", "File exists: ${file.exists()}, size: ${file.length()}")
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = hashMapOf(
                "messageId" to messageId,
                "chatId" to chatId,
                "content" to downloadUrl,
                "senderId" to currentUserId,
                "timestamp" to System.currentTimeMillis(),
                "type" to "voice"
            )

            messageDocRef.set(message).await()

            chatDocRef.update(
                mapOf(
                    "lastMessage" to "[Voice message]",
                    "lastMessageTimestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to upload voice message", e)
        }
    }




    // Listen for new messages from Firestore and insert into Room
    fun listenForMessages(chatId: String) {
        messagesListenerRegistration?.remove()  // Remove previous listener if any

        messagesListenerRegistration = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    for (docChange in snapshots.documentChanges) {
                        if (docChange.type == DocumentChange.Type.ADDED) {
                            val msg = docChange.document

                            val messageEntity = MessageEntity(
                                messageId = msg.getString("messageId") ?: msg.id,
                                chatId = chatId,
                                content = msg.getString("content") ?: "",
                                isSentByMe = msg.getString("senderId") == FirebaseAuth.getInstance().currentUser?.uid,
                                timestamp = msg.getLong("timestamp") ?: 0L,
                                senderId = msg.getString("senderId") ?: "", // <-- ADD THIS
                                voiceUrl = msg.getString("voiceUrl")
                            )


                            CoroutineScope(Dispatchers.IO).launch {
                                messageDao.insertMessage(messageEntity)
                            }
                        }
                    }
                }
            }
    }



    // Get messages from Room
    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }
    fun getChatsForUser(userId: String): Flow<List<Chat>> = callbackFlow {
        chatsListenerRegistration?.remove()

        chatsListenerRegistration = firestore.collection("chats")
            .whereArrayContains("members", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val chatList = snapshots.documents.map { doc ->
                            val members = doc.get("members") as? List<String> ?: emptyList()
                            val otherUserId = members.firstOrNull { it != userId }

                            val otherUserName = if (otherUserId != null) {
                                try {
                                    firestore.collection("users").document(otherUserId).get().await()
                                        .getString("email") ?: "Unknown"
                                } catch (e: Exception) {
                                    "Unknown"
                                }
                            } else "Unknown"

                            Chat(
                                id = doc.id,
                                name = otherUserName,
                                lastMessage = doc.getString("lastMessage") ?: "",
                                time = formatTimestamp(doc.getLong("lastMessageTimestamp")),
                                unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                                members = members
                            )
                        }
                        trySend(chatList).isSuccess
                    }
                }
            }

        awaitClose {
            chatsListenerRegistration?.remove()
            chatsListenerRegistration = null
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return ""
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("HH:mm")
        return formatter.format(date)
    }

    suspend fun getUserEmail(userId: String): String? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            val email = snapshot.getString("email")
            Log.d("ChatRepository", "getUserEmail: userId=$userId, email=$email")
            email
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching email for userId=$userId", e)
            null
        }
    }


    fun isGroupChat(chatId: String): Boolean {
        // Basic logic: if the chatId starts with "group_" or has >2 members, etc.
        return chatId.startsWith("group_")
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.delete(message)
    }


}


