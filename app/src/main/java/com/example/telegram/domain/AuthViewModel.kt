package com.example.telegram.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = AuthUiState.Error("Email and password must not be empty")
            return
        }
        if (password.length < 6) {
            uiState = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        uiState = AuthUiState.Loading

        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        val userData = mapOf(
                            "email" to user.email,
                            "name" to "",
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                uiState = AuthUiState.Success(user.uid)
                            }
                            .addOnFailureListener {
                                uiState = AuthUiState.Success(user.uid) // still allow login
                            }
                    } else {
                        uiState = AuthUiState.Error("User is null after sign up")
                    }
                } else {
                    uiState = AuthUiState.Error(task.exception?.localizedMessage ?: "Sign-up failed")
                }
            }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = AuthUiState.Error("Email and password must not be empty")
            return
        }

        uiState = AuthUiState.Loading

        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        messaging.token.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val token = tokenTask.result
                                if (!token.isNullOrEmpty()) {
                                    firestore.collection("users").document(user.uid)
                                        .update("fcmToken", token)
                                        .addOnSuccessListener {
                                            uiState = AuthUiState.Success(user.uid)
                                        }
                                        .addOnFailureListener {
                                            uiState = AuthUiState.Success(user.uid) // still continue
                                        }
                                } else {
                                    uiState = AuthUiState.Success(user.uid)
                                }
                            } else {
                                uiState = AuthUiState.Success(user.uid) // token optional
                            }
                        }
                    } else {
                        uiState = AuthUiState.Error("User is null after login")
                    }
                } else {
                    uiState = AuthUiState.Error(task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    fun resetUiState() {
        uiState = AuthUiState.Idle
    }

    fun logout(navController: NavController) {
        auth.signOut()
        uiState = AuthUiState.Idle

        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }
}
