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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val name: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0
)

enum class ProfileStatus {
    IDLE, LOADING, INCOMPLETE, COMPLETE
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")
    
    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val _profileStatus = MutableStateFlow(ProfileStatus.IDLE)
    val profileStatus = _profileStatus.asStateFlow()

    init {
        // PRO LOGIC: Gunakan Listener agar UI selalu sinkron dengan Firebase
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                fetchUserProfile()
            } else {
                _profileStatus.value = ProfileStatus.IDLE
            }
        }
    }

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        _profileStatus.value = ProfileStatus.LOADING
        viewModelScope.launch {
            try {
                val snapshot = db.getReference("users").child(uid).child("profile").get().await()
                val profile = snapshot.getValue(UserProfile::class.java)
                
                if (profile != null && profile.weight > 0 && profile.height > 0) {
                    _userProfile.value = profile
                    _profileStatus.value = ProfileStatus.COMPLETE
                } else {
                    val googleName = auth.currentUser?.displayName ?: ""
                    _userProfile.value = UserProfile(name = googleName)
                    _profileStatus.value = ProfileStatus.INCOMPLETE
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching profile", e)
                _profileStatus.value = ProfileStatus.INCOMPLETE
            }
        }
    }

    fun saveUserProfile(name: String, weight: Double, height: Double, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val profile = UserProfile(name, weight, height)
        viewModelScope.launch {
            try {
                db.getReference("users").child(uid).child("profile").setValue(profile).await()
                _userProfile.value = profile
                _profileStatus.value = ProfileStatus.COMPLETE
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // Fungsi manual tetap ada untuk trigger UI jika perlu
    fun updateCurrentUser() {
        _currentUser.value = auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }

    fun loginWithGoogle(context: Context, onError: (String) -> Unit) {
        val credentialManager = CredentialManager.create(context)
        val webClientId = context.getString(R.string.default_web_client_id)

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
                if (credential is GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    // AuthStateListener di init akan menghandle sisa alurnya
                }
            } catch (e: Exception) {
                onError("Gagal Login Google: ${e.message}")
            }
        }
    }
}
