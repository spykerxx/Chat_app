package com.example.telegram.domain


sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userId: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
