package com.example.fitme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.ui.theme.FitMeTheme

class MainActivity : ComponentActivity() {
    
    // Inisialisasi Database & Repository di tingkat Activity agar tetap persisten
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitMeTheme {

            }
        }
    }
}
