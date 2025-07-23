package com.example.telegram.screens

import android.Manifest
import android.media.MediaRecorder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.telegram.domain.AuthViewModel
import com.example.telegram.data.repository.ChatDatabase
import com.example.telegram.data.repository.ChatRepository
import com.example.telegram.domain.ChatViewModel
import com.example.telegram.domain.ChatViewModelFactory
import com.example.telegram.R
import com.example.telegram.data.model.VoiceMessagePlayer
import com.example.telegram.data.local.Chat
import com.example.telegram.data.local.Message
import com.example.telegram.data.utils.createAudioFilePath
import com.example.telegram.data.utils.formatTimestamp
import com.example.telegram.data.utils.hasAudioPermission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMainScreen(navController: NavController) {
    val context = LocalContext.current
    val db = ChatDatabase.Companion.getDatabase(context)
    val repository = remember { ChatRepository(db.messageDao()) }
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repository))

    val chatList by viewModel.chats.collectAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    // States for New Chat dialog (FAB)
    var showNewChatDialog by remember { mutableStateOf(false) }
    var receiverEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // States for Search dialog (TopBar search icon)
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchEmail by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menu", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)

                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("account")
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                )

                val authViewModel: AuthViewModel = viewModel()
                NavigationDrawerItem(

                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        authViewModel.logout(navController)
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ChatApp") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchEmail = ""
                            searchError = null
                            showSearchDialog = true
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Chat")
                }
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(chatList) { chat ->
                        ChatItem(chat, navController)
                    }
                }
            }
        )
    }


    // New Chat dialog (FAB)
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewChatDialog = false
                receiverEmail = ""
                errorMessage = null
                isLoading = false
            },
            title = { Text("Start New Chat") },
            text = {
                Column {
                    OutlinedTextField(
                        value = receiverEmail,
                        onValueChange = { receiverEmail = it },
                        label = { Text("Enter receiver's email") },
                        singleLine = true,
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (receiverEmail.isBlank()) {
                            errorMessage = "Email cannot be empty"
                            return@TextButton
                        }
                        if (receiverEmail.equals(currentUser?.email, ignoreCase = true)) {
                            errorMessage = "You cannot chat with yourself"
                            return@TextButton
                        }

                        errorMessage = null
                        isLoading = true

                        val firestore = FirebaseFirestore.getInstance()

                        firestore.collection("users")
                            .whereEqualTo("email", receiverEmail.trim())
                            .get()
                            .addOnSuccessListener { snapshot ->
                                isLoading = false
                                if (!snapshot.isEmpty) {
                                    val receiverUid = snapshot.documents[0].id

                                    val newChatRef = firestore.collection("chats").document()
                                    val chatData = hashMapOf(
                                        "name" to receiverEmail,
                                        "lastMessage" to "",
                                        "lastMessageTimestamp" to 0L,
                                        "members" to listOf(currentUserId, receiverUid)
                                    )
                                    newChatRef.set(chatData)
                                        .addOnSuccessListener {
                                            showNewChatDialog = false
                                            receiverEmail = ""
                                            navController.navigate("chat/${newChatRef.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            errorMessage = "Failed to create chat: ${e.localizedMessage}"
                                        }
                                } else {
                                    errorMessage = "No user found with this email"
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "Error searching user: ${e.localizedMessage}"
                            }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Start Chat")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewChatDialog = false
                    receiverEmail = ""
                    errorMessage = null
                    isLoading = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Search Chat dialog (TopBar search)
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = {
                showSearchDialog = false
                searchEmail = ""
                searchError = null
                isSearching = false
            },
            title = { Text("Search Chat by Email") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchEmail,
                        onValueChange = {
                            searchEmail = it
                            searchError = null
                        },
                        label = { Text("Enter email to search") },
                        singleLine = true,
                        isError = searchError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (searchError != null) {
                        Text(
                            text = searchError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (searchEmail.isBlank()) {
                            searchError = "Please enter an email"
                            return@TextButton
                        }
                        if (searchEmail.equals(currentUser?.email, ignoreCase = true)) {
                            searchError = "You cannot search for yourself"
                            return@TextButton
                        }

                        searchError = null
                        isSearching = true

                        val firestore = FirebaseFirestore.getInstance()

                        firestore.collection("users")
                            .whereEqualTo("email", searchEmail.trim())
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                if (!userSnapshot.isEmpty) {
                                    val receiverUid = userSnapshot.documents[0].id

                                    firestore.collection("chats")
                                        .whereArrayContains("members", currentUserId!!)
                                        .get()
                                        .addOnSuccessListener { chatsSnapshot ->
                                            val existingChat = chatsSnapshot.documents.find { doc ->
                                                val members = doc.get("members") as? List<*>
                                                members?.contains(receiverUid) == true
                                            }

                                            isSearching = false
                                            if (existingChat != null) {
                                                showSearchDialog = false
                                                searchEmail = ""
                                                searchError = null
                                                navController.navigate("chat/${existingChat.id}")
                                            } else {
                                                searchError = "No existing chat found with this user"
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isSearching = false
                                            searchError = "Error finding chats: ${e.localizedMessage}"
                                        }
                                } else {
                                    isSearching = false
                                    searchError = "No user found with this email"
                                }
                            }
                            .addOnFailureListener { e ->
                                isSearching = false
                                searchError = "Error searching user: ${e.localizedMessage}"
                            }
                    },
                    enabled = !isSearching
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Search")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSearchDialog = false
                    searchEmail = ""
                    searchError = null
                    isSearching = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// -------------------- Chat Item --------------------
@Composable
fun ChatItem(chat: Chat, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("chat/${chat.id}")
            }

            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.avatar),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.name, style = MaterialTheme.typography.titleMedium)
            Text(text = chat.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = chat.time, style = MaterialTheme.typography.labelSmall)
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .background(Color.Black, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}


// -------------------- Contact Chat Screen --------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chat: Chat, navController: NavController) {
    val context = LocalContext.current
    val db = ChatDatabase.Companion.getDatabase(context)
    val repository = remember { ChatRepository(db.messageDao()) }
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repository))

    LaunchedEffect(Unit) {
        viewModel.selectChat(chat.id)
    }

    val messages by viewModel.messages.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // States declared first so startRecording can use them
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFilePath by remember { mutableStateOf<String?>(null) }

    // startRecording function uses above states
    fun startRecording() {
        audioFilePath = createAudioFilePath(context)
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            prepare()
            start()
        }
        isRecording = true
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                coroutineScope.launch {
                    startRecording()
                }
            } else {
                Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.avatar), // default avatar
                            contentDescription = "Contact Avatar",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = chat.name)
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    singleLine = true,
                    enabled = !isRecording
                )

                IconButton(
                    onClick = {
                        if (context.hasAudioPermission()) {
                            if (!isRecording) {
                                startRecording()
                            } else {
                                // Stop recording
                                recorder?.apply {
                                    stop()
                                    release()
                                }
                                recorder = null
                                isRecording = false
                                audioFilePath?.let { path ->
                                    viewModel.sendVoiceMessage(path)
                                }
                            }
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Record Voice"
                    )
                }

                if (!isRecording && inputText.trim().isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val messageToSend = inputText.trim()
                            if (messageToSend.isNotEmpty()) {
                                viewModel.sendMessage(messageToSend)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 8.dp),
            reverseLayout = false
        ) {
            items(messages.reversed()) { entity ->
                val message = Message(
                    content = entity.content,
                    isSentByMe = entity.isSentByMe,
                    timestamp = entity.timestamp
                )
                MessageBubble(message = message, showSenderName = false)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}


// ---------------- Message Bubble -----------------
@Composable
fun MessageBubble(message: Message, showSenderName: Boolean = false) {
    val backgroundColor = if (message.isSentByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        if (showSenderName && !message.isSentByMe) {
            Text(
                text = message.senderName ?: "(Unknown sender)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor,
            tonalElevation = 2.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            if (message.content.startsWith("https://") && message.content.endsWith(".m4a")) {
                VoiceMessagePlayer(audioUrl = message.content)
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
