package com.example.fitme.frontEnd

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.ui.theme.PrimaryNeon
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleLogin: (String) -> Unit // Minta String (Token), bukan Unit kosong
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // --- GOOGLE SIGN IN CLIENT SETUP ---
    val webClientId = "621078401507-h0h0bpcq125hbn4asqr89098d6f8tbpn.apps.googleusercontent.com"
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    onGoogleLogin(idToken) // Lempar token ke MainActivity
                } else {
                    Toast.makeText(context, "Google Token Kosong", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Login Gagal: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(PrimaryNeon.copy(alpha = 0.1f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = PrimaryNeon
            ) {
                Box(contentAlignment = Alignment.Center) { Text("F", color = Color.Black, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Welcome Back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("Push your limits today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryNeon) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryNeon) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // FORCE RELOAD STATUS VERIFIKASI
                                    auth.currentUser?.reload()?.addOnCompleteListener {
                                        isLoading = false
                                        if (auth.currentUser?.isEmailVerified == true) {
                                            onLoginSuccess()
                                        } else {
                                            Toast.makeText(context, "Verify your email!", Toast.LENGTH_SHORT).show()
                                            auth.signOut()
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, task.exception?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                else Text("SIGN IN", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    // Signout dulu biar selalu muncul pilihan akun
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CONTINUE WITH GOOGLE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onNavigateToRegister) {
                Row {
                    Text("New here? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Create Account", color = PrimaryNeon, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}