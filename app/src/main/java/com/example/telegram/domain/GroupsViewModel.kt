package com.example.telegram.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegram.data.local.Group
import com.example.telegram.data.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()  // better to expose as immutable
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroupById(groupId).collect { group ->
                _currentGroup.value = group
            }
        }
    }

    init {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            groupRepository.getGroupsForUser(userId)
                .collect { groupList ->
                    _groups.value = groupList
                }
        }
    }

    fun addGroup(name: String, members: List<String>) {
        viewModelScope.launch {
            val result = groupRepository.createGroup(name, members)
            // handle success/failure if needed
        }
    }

    suspend fun getUserEmail(userId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("email") ?: "No Email"
        } catch (e: Exception) {
            "Error loading email"
        }
    }

    suspend fun getUserIdByEmail(email: String): String? {
        return try {
            groupRepository.getUserIdByEmail(email)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMemberToGroup(groupId: String, userId: String) {
        try {
            groupRepository.addMemberToGroup(groupId, userId)
            // Optionally, reload group data to refresh UI
            loadGroup(groupId)
        } catch (e: Exception) {
            // Handle error (e.g., log or show message)
        }
    }



}
