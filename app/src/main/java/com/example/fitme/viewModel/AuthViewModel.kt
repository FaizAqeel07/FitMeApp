package com.example.fitme.viewModel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Field

data class UserProfile(
    val name: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0
)

enum class ProfileStatus {
    IDLE, LOADING, INCOMPLETE, COMPLETE
}

/**
 * SOLID: AuthViewModel handles user authentication and profile state.
 * SECURITY: Database URL and Client ID are secured via Reflection-Safe BuildConfig access.
 */
class AuthViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // SECURITY: Helper to read BuildConfig safely even if generation is lagging
    private fun getSafeBuildConfigValue(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.fitme.BuildConfig")
            val field: Field = clazz.getField(fieldName)
            field.get(null) as String
        } catch (e: Exception) {
            ""
        }
    }

    private val firebaseDatabase: FirebaseDatabase by lazy {
        val url = getSafeBuildConfigValue("FIREBASE_DATABASE_URL")
        if (url.isNotEmpty()) FirebaseDatabase.getInstance(url) else FirebaseDatabase.getInstance()
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val _profileStatus = MutableStateFlow(ProfileStatus.IDLE)
    val profileStatus = _profileStatus.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile = _isSavingProfile.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            if (user != null) {
                fetchUserProfile()
            } else {
                _profileStatus.value = ProfileStatus.IDLE
            }
        }
    }

    fun fetchUserProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        _profileStatus.value = ProfileStatus.LOADING
        viewModelScope.launch {
            try {
                val snapshot = firebaseDatabase.getReference("users").child(uid).child("profile").get().await()
                // FIX: Explicit typing for Firebase getValue
                val profile = snapshot.getValue(UserProfile::class.java)

                if (profile != null && profile.weight > 0 && profile.height > 0) {
                    _userProfile.value = profile
                    _profileStatus.value = ProfileStatus.COMPLETE
                } else {
                    val googleName = firebaseAuth.currentUser?.displayName ?: ""
                    _userProfile.value = UserProfile(name = googleName)
                    _profileStatus.value = ProfileStatus.INCOMPLETE
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching profile", e)
                _profileStatus.value = ProfileStatus.INCOMPLETE
            }
        }
    }

    fun saveUserProfile(name: String, weight: Double, height: Double, onResult: (Boolean) -> Unit) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val profile = UserProfile(name, weight, height)
        
        viewModelScope.launch {
            _isSavingProfile.value = true
            try {
                firebaseDatabase.getReference("users").child(uid).child("profile").setValue(profile).await()
                _userProfile.value = profile
                _profileStatus.value = ProfileStatus.COMPLETE
                onResult(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving profile", e)
                onResult(false)
            } finally {
                _isSavingProfile.value = false
            }
        }
    }

    fun updateCurrentUser() {
        _currentUser.value = firebaseAuth.currentUser
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun loginWithGoogle(context: Context, onError: (String) -> Unit) {
        val credentialManager = CredentialManager.create(context)
        val webClientId = getSafeBuildConfigValue("GOOGLE_WEB_CLIENT_ID")

        if (webClientId.isEmpty()) {
            onError("Authentication configuration missing. Please rebuild project.")
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                    firebaseAuth.signInWithCredential(firebaseCredential).await()
                    updateCurrentUser()
                } else {
                    onError("Unrecognized credential type")
                }
            } catch (e: Exception) {
                onError("Google Login Failed: ${e.message}")
            }
        }
    }
}
