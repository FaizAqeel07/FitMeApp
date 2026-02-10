package com.example.fitme.viewModel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    fun updateCurrentUser() {
        _currentUser.value = auth.currentUser
    }

    fun signOut() {
        auth.signOut()
        updateCurrentUser()
    }

    private fun processFirebaseSignIn(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateCurrentUser()
                    onSuccess()
                } else {
                    Log.e("AuthViewModel", "Firebase Auth failed", task.exception)
                    onError(task.exception?.message ?: "Firebase Auth failed")
                }
            }
    }

    fun loginWithGoogle(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credentialManager = CredentialManager.create(context)
        
        val webClientId = context.getString(R.string.default_web_client_id)
        Log.d("AuthViewModel", "Using Web Client ID: $webClientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                Log.d("AuthViewModel", "Got credential type: ${credential.type}")

                if (credential is GoogleIdTokenCredential) {
                    processFirebaseSignIn(credential.idToken, onSuccess, onError)
                } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        processFirebaseSignIn(googleIdTokenCredential.idToken, onSuccess, onError)
                    } catch (e: Exception) {
                        onError("Gagal memproses data Google: ${e.message}")
                    }
                } else {
                    onError("Jenis kredensial tidak dikenali: ${credential.type}")
                }
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential Error: ${e.type}", e)
                val userFriendlyError = when(e.type) {
                    "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> "Login dibatalkan"
                    "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> "Tidak ada akun Google. Cek SHA-1 & Google Play Services."
                    else -> "Gagal: ${e.message}"
                }
                onError(userFriendlyError)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error", e)
                onError("Kesalahan sistem: ${e.message}")
            }
        }
    }
}
