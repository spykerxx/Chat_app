package com.example.telegram.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.telegram.domain.ChatViewModel
import com.example.telegram.domain.GroupsViewModel
import com.example.telegram.data.local.Group
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groupsViewModel: GroupsViewModel,
    onBackClick: () -> Unit = {},
    onGroupClick: (Group) -> Unit = {}
) {
    val groups by groupsViewModel.groups.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: handle search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Group")
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(groups) { group ->
                    GroupItem(group = group, onClick = { onGroupClick(group) })
                }
            }
        }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create New Group") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank() && currentUserId != null) {
                            groupsViewModel.addGroup(
                                newGroupName.trim(),
                                members = listOf(currentUserId)
                            )
                            newGroupName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
fun GroupItem(group: Group, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .background(Color.LightGray, CircleShape)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = group.name, style = MaterialTheme.typography.titleMedium)
            Text(text = group.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text(text = "${group.members.size} members", style = MaterialTheme.typography.labelSmall)

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    groupName: String,
    navBack: () -> Unit,
    chatViewModel: ChatViewModel,
    groupsViewModel: GroupsViewModel
) {
    LaunchedEffect(groupId) {
        chatViewModel.selectChat(groupId)
        groupsViewModel.loadGroup(groupId)
    }

    val messages by chatViewModel.messages.collectAsState()
    val group by groupsViewModel.currentGroup.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showMembersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = navBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Members")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message, showSenderName = true)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message") },
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    )

    if (showMembersDialog) {
        GroupMembersDialog(
            memberIds = group?.members ?: emptyList(),
            onDismiss = { showMembersDialog = false },
            groupsViewModel = groupsViewModel,
            groupId = groupId
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersDialog(
    memberIds: List<String>,
    onDismiss: () -> Unit,
    groupsViewModel: GroupsViewModel,
    groupId: String // You need groupId to add members
) {
    var memberEmails by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAddMemberInput by remember { mutableStateOf(false) }
    var newMemberEmail by remember { mutableStateOf("") }
    var addMemberError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(memberIds) {
        val emails = mutableMapOf<String, String>()
        for (id in memberIds) {
            emails[id] = groupsViewModel.getUserEmail(id)
        }
        memberEmails = emails
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Members") },
        text = {
            Column {
                if (memberIds.isEmpty()) {
                    Text("No members found.")
                } else {
                    memberIds.forEach { id ->
                        Text(text = memberEmails[id] ?: id)
                    }
                }

                if (showAddMemberInput) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = newMemberEmail,
                        onValueChange = {
                            newMemberEmail = it
                            addMemberError = null
                        },
                        label = { Text("Enter member email") },
                        singleLine = true,
                        isError = addMemberError != null
                    )
                    if (addMemberError != null) {
                        Text(
                            text = addMemberError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (showAddMemberInput) {
                            // Handle add member logic
                            if (newMemberEmail.isBlank()) {
                                addMemberError = "Email cannot be empty"
                                return@TextButton
                            }
                            isAdding = true
                            // Try to get user ID by email
                            // Assuming getUserIdByEmail is suspend and returns String? or null if not found
                            groupsViewModel.viewModelScope.launch {
                                val userId = groupsViewModel.getUserIdByEmail(newMemberEmail.trim())
                                if (userId == null) {
                                    addMemberError = "User not found"
                                } else if (memberIds.contains(userId)) {
                                    addMemberError = "User is already a member"
                                } else {
                                    groupsViewModel.addMemberToGroup(groupId, userId)
                                    newMemberEmail = ""
                                    addMemberError = null
                                    showAddMemberInput = false
                                }
                                isAdding = false
                            }
                        } else {
                            showAddMemberInput = true
                        }
                    },
                    enabled = !isAdding
                ) {
                    Text(if (showAddMemberInput) "Add" else "Add Member")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}








