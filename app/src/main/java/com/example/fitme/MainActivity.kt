package com.example.fitme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.Database.AppDatabase
import com.example.fitme.Database.FitTrackNavGraph
import com.example.fitme.RepositoryViewModel.FitTrackRepository
import com.example.fitme.RepositoryViewModel.ViewModelFactory
import com.example.fitme.ui.theme.FitMeTheme

class MainActivity : ComponentActivity() {
    
    // Inisialisasi Database & Repository di tingkat Activity agar tetap persisten
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { FitTrackRepository(database.fitTrackDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitMeTheme {
                // Buat ViewModel menggunakan Factory dengan repository yang sudah diinisialisasi
                val viewModel: com.example.fitme.RepositoryViewModel.FitTrackViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )

                // Gunakan NavGraph untuk mengelola semua layar
                FitTrackNavGraph(viewModel = viewModel)
            }
        }
    }
}
