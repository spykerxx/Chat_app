package com.example.telegram.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.telegram.domain.AuthUiState
import com.example.telegram.domain.AuthViewModel
import com.example.telegram.data.remote.GoogleAuthUiClient
import com.example.telegram.R
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    context as Activity

    val oneTapClient = remember { Identity.getSignInClient(context) }
    val googleAuthUiClient = remember { GoogleAuthUiClient(context, oneTapClient) }

    val uiState by remember { derivedStateOf { viewModel.uiState } }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val user = googleAuthUiClient.signInWithIntent(result.data!!)
                        withContext(Dispatchers.Main) {
                            if (user != null) {
                                navController.navigate("chatMain") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Handle error if needed
                    }
                }
            }
        }
    )

    // Navigate on successful sign-up
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            navController.navigate("chatMain") {
                popUpTo("signup") { inclusive = true }
            }
            viewModel.resetUiState()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            if (uiState is AuthUiState.Error) {
                Text((uiState as AuthUiState.Error).message, color = Color.Red)
            }

            Button(
                onClick = { viewModel.signUp(email, password) },
                enabled = uiState != AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState == AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Sign Up")
                }
            }

            OutlinedButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = googleAuthUiClient.signIn()
                            withContext(Dispatchers.Main) {
                                launcher.launch(
                                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                )
                            }
                        } catch (_: Exception) {}
                    }
                },
                enabled = uiState != AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.googlelogo),
                    contentDescription = "Google Sign-In",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Up with Google")
            }

            TextButton(onClick = {
                navController.navigate("login")
            }) {
                Text("Already have an account? Log in")
            }
        }
    }
}
