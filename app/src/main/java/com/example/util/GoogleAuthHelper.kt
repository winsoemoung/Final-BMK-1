package com.example.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {
    private const val WEB_CLIENT_ID = "223700756175-v2k0spsedfgo5aninh9qr6tp5o8afsh5.apps.googleusercontent.com"

    suspend fun signInWithGoogle(context: Context): Result<String> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val user = authResult.user
                val displayName = user?.displayName ?: user?.email ?: "Google Account"
                Result.success(displayName)
            } else {
                Result.failure(Exception("မထောက်ပံ့သော Credential အမျိုးအစားဖြစ်သည်"))
            }
        } catch (e: NoCredentialException) {
            Result.failure(Exception("Google အကောင့် မတွေ့ရှိပါ သို့မဟုတ် ရွေးချယ်မှု မရှိပါ"))
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google sign-in ကို အသုံးပြုသူမှ ပယ်ဖျက်လိုက်ပါသည်"))
        } catch (e: GetCredentialException) {
            Result.failure(Exception(e.message ?: "Google Sign-in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Throwable) {
            // Ignore signout error if Firebase is uninitialized
        }
    }

    fun getCurrentUser(): com.google.firebase.auth.FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Throwable) {
            null
        }
    }
}
