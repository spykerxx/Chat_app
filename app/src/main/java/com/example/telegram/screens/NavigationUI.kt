package com.example.telegram.screens

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.telegram.domain.AuthViewModel
import com.example.telegram.data.repository.ChatDatabase
import com.example.telegram.data.repository.ChatRepository
import com.example.telegram.domain.ChatViewModel
import com.example.telegram.domain.ChatViewModelFactory
import com.example.telegram.data.repository.GroupRepository
import com.example.telegram.domain.GroupsViewModel
import com.example.telegram.domain.GroupsViewModelFactory
import com.example.telegram.domain.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun App() {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val showBottomBar = currentDestination?.startsWith("chat") == true ||
            currentDestination == "groups" ||
            currentDestination == "settings"

    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) "chatMain" else "login"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController, currentDestination)
            }
        }
    ) { innerPadding ->
        AppNavHost(navController, Modifier.padding(innerPadding), startDestination)
    }
}

@Composable
fun BottomBar(navController: NavController, currentDestination: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == "chatMain",
            onClick = { navController.navigate("chatMain") },
            icon = { Icon(Icons.Filled.Menu, contentDescription = "Chats") },
            label = { Text("Chats") }
        )
        NavigationBarItem(
            selected = currentDestination == "groups",
            onClick = { navController.navigate("groups") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Groups") },
            label = { Text("Groups") }
        )
        NavigationBarItem(
            selected = currentDestination == "settings",
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String
) {
    val context = LocalContext.current.applicationContext as Application
    val settingsViewModel: SettingsViewModel = viewModel(factory =
        ViewModelProvider.AndroidViewModelFactory.getInstance(context)
    )

    val isDarkMode by settingsViewModel.darkModeFlow.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
        composable("chatMain") { ChatMainScreen(navController) }

        composable("groups") {
            val context = LocalContext.current.applicationContext as Application
            val firestore = FirebaseFirestore.getInstance()
            val groupRepository = remember { GroupRepository(firestore) }
            val groupsViewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModelFactory(groupRepository)
            )

            GroupsScreen(
                groupsViewModel = groupsViewModel,
                onGroupClick = { group ->
                    // Navigate to group chat screen
                    navController.navigate("groupChat/${group.id}/${group.name}")
                }
            )
        }

        composable("settings") {
            val authViewModel: AuthViewModel = viewModel()
            SettingsScreen(
                navController = navController,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { enabled ->
                    settingsViewModel.toggleDarkMode(enabled)
                },

                authViewModel = authViewModel
            )
        }

        composable("account") { AccountScreen(navController) }

        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")

            val db = ChatDatabase.Companion.getDatabase(context)
            val repository = remember { ChatRepository(db.messageDao()) }
            val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repository))

            val chats by chatViewModel.chats.collectAsState()
            val chat = chats.firstOrNull { it.id == chatId }

            chat?.let {
                ChatScreen(chat, navController)
            }
        }

        // ✅ Add this block below the user chat screen
        composable("groupChat/{groupId}/{groupName}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupName = backStackEntry.arguments?.getString("groupName") ?: "Group Chat"

            val context = LocalContext.current.applicationContext as Application
            val firestore = FirebaseFirestore.getInstance()
            val groupRepository = remember { GroupRepository(firestore) }
            val groupsViewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModelFactory(groupRepository)
            )

            val db = ChatDatabase.Companion.getDatabase(context)
            val repository = remember { ChatRepository(db.messageDao()) }
            val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repository))

            GroupChatScreen(
                groupId = groupId,
                groupName = groupName,
                navBack = { navController.popBackStack() },
                chatViewModel = chatViewModel,
                groupsViewModel = groupsViewModel  // <---- Pass it here!
            )
        }
    }

}
