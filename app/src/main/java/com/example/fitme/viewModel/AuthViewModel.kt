package com.example.fitme.viewModel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class AuthViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private fun getSafeBuildConfigValue(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.fitme.BuildConfig")
            val field: Field = clazz.getField(fieldName)
            val value = field.get(null) as String
            value
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

    // --- PRO FIX: Variabel ini dibalikin lagi biar UI gak error ---
    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile = _isSavingProfile.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut = _isLoggingOut.asStateFlow()
    // --------------------------------------------------------------

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
        viewModelScope.launch {
            _isLoggingOut.value = true
            try {
                firebaseAuth.signOut()
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                onComplete()
            } catch (e: Exception) {
                onComplete()
            } finally {
                _isLoggingOut.value = false
            }
        }
    }

    // Fungsi Sign In Google (Terima Token dari UI)
    fun signInWithGoogleToken(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()

                // Reload user untuk memastikan data fresh
                authResult.user?.reload()?.await()

                updateCurrentUser()
                onSuccess()
            } catch (e: Exception) {
                onError("Login Gagal: ${e.message}")
            }
        }
    }
}