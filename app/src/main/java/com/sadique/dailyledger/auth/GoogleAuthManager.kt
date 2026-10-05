package com.sadique.dailyledger.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Firebase verifies the token; locally decoded claims are never used as authentication. */
class GoogleAuthManager(private val context: Context) {
    suspend fun idToken(clientId: String): String {
        require(clientId.isNotBlank()) { "Google sign-in setup is pending for this app." }
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        require(credential is CustomCredential && (
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
            )) { "Google did not return a sign-in credential. Please try again." }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    suspend fun clear() {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
