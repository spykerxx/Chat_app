package com.example.telegram.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.telegram.domain.AuthUiState
import com.example.telegram.domain.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uiState = authViewModel.uiState

    // React to UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Loading -> {
                errorMessage = null
            }
            is AuthUiState.Success -> {
                errorMessage = null
                navController.navigate("chatMain") {
                    popUpTo("login") { inclusive = true }
                }
                authViewModel.resetUiState()
            }
            is AuthUiState.Error -> {
                errorMessage = uiState.message
                authViewModel.resetUiState()
            }
            else -> { /* Idle, do nothing */ }
        }
    }

    val loading = uiState is AuthUiState.Loading

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Text("Welcome Back", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red)
            }

            Button(
                onClick = {
                    authViewModel.login(email.trim(), password.trim())
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Log In")
                }
            }

            TextButton(onClick = {
                navController.navigate("signup")
            }) {
                Text("Don't have an account? Sign up")
            }
        }
    }
}