package com.example.telegram.data.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.telegram.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(): SignInResult {
        val beginSignInResult = oneTapClient.beginSignIn(
            BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .setAutoSelectEnabled(true)
                .build()
        ).await()

        return SignInResult(pendingIntent = beginSignInResult.pendingIntent)
    }


    suspend fun signInWithIntent(intent: Intent): FirebaseUser? {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleCredential = GoogleAuthProvider.getCredential(credential.googleIdToken, null)
        return auth.signInWithCredential(googleCredential).await().user
    }

}

data class SignInResult(val pendingIntent: PendingIntent)

