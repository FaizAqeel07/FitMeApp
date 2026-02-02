package com.example.fitme

import android.app.Application
import com.example.fitme.Database.AppDatabase
import com.example.fitme.RepositoryViewModel.FitTrackRepository

class FitMeApplication : Application() {
    // Inisialisasi Database dan Repository secara lazy
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { FitTrackRepository(database.fitTrackDao()) }
}
