package com.example.telegram.data.repository

import com.example.telegram.data.local.Group
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository(private val firestore: FirebaseFirestore) {

    fun getGroupsForUser(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(groups).isSuccess
            }
        awaitClose { listener.remove() }
    }

    suspend fun createGroup(name: String, members: List<String>): Result<Unit> {
        return try {
            val data = mapOf(
                "name" to name,
                "members" to members,
                "lastMessage" to "",
                "lastMessageTimestamp" to 0L
            )
            firestore.collection("groups").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGroupById(groupId: String): Flow<Group?> = callbackFlow {
        val listenerRegistration = firestore.collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val group = snapshot?.toObject(Group::class.java)
                trySend(group).isSuccess
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getUserEmail(userId: String): String? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getString("email")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserIdByEmail(email: String): String? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMemberToGroup(groupId: String, userId: String) {
        try {
            val groupRef = firestore.collection("groups").document(groupId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(groupRef)
                val currentMembers = snapshot.get("members") as? List<String> ?: emptyList()
                if (!currentMembers.contains(userId)) {
                    val updatedMembers = currentMembers + userId
                    transaction.update(groupRef, "members", updatedMembers)
                }
            }.await()
        } catch (e: Exception) {
            // Handle error
        }
    }


}
