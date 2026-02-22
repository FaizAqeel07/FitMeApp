package com.example.fitme.viewModel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
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
 */
class AuthViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    private fun getSafeBuildConfigValue(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.fitme.BuildConfig")
            val field: Field = clazz.getField(fieldName)
            val value = field.get(null) as String
            Log.d("AuthViewModel", "BuildConfig Value for $fieldName: $value")
            value
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error reading BuildConfig $fieldName", e)
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

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut = _isLoggingOut.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            if (user != null) {
                fetchUserProfile()
            } else {
                _profileStatus.value = ProfileStatus.IDLE
                _isReady.value = true
            }
        }
    }

    fun fetchUserProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        _profileStatus.value = ProfileStatus.LOADING
        viewModelScope.launch {
            try {
                val snapshot = firebaseDatabase.getReference("users").child(uid).child("profile").get().await()
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
            } finally {
                _isReady.value = true
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

    fun signOutWithSync(context: Context, onComplete: () -> Unit) {
        val uid = firebaseAuth.currentUser?.uid
        val currentProfile = _userProfile.value
        
        viewModelScope.launch {
            _isLoggingOut.value = true
            try {
                if (uid != null && currentProfile.weight > 0) {
                    firebaseDatabase.getReference("users").child(uid).child("profile").setValue(currentProfile).await()
                }
                firebaseAuth.signOut()
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                onComplete()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during sign out sync", e)
                onComplete()
            } finally {
                _isLoggingOut.value = false
            }
        }
    }

    fun loginWithGoogle(context: Context, onError: (String) -> Unit) {
        val credentialManager = CredentialManager.create(context)
        
        // Final Client ID used based on your confirmation
        val webClientId = "621078401507-h0h0bpcq125hbn4asqr89098d6f8tbpn.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false) // Keep false to avoid UNAVAILABLE auto-signin helper errors
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Google Login Call Started. Client ID: $webClientId")
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    if (!idToken.isNullOrEmpty()) {
                        Log.d("AuthViewModel", "Successfully retrieved Google ID Token")
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        firebaseAuth.signInWithCredential(firebaseCredential).await()
                        updateCurrentUser()
                    } else {
                        Log.e("AuthViewModel", "Received empty ID token")
                        onError("Google Token is empty. Check OAuth Consent Screen status.")
                    }
                } else {
                    onError("Unrecognized credential type: ${credential.type}")
                }
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential Manager Exception: ${e.message}", e)
                onError("Login Cancelled or Failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "General Login Exception", e)
                onError("Login Error: ${e.localizedMessage}")
            }
        }
    }
}