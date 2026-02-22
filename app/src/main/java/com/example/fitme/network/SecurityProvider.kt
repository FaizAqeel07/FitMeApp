package com.example.fitme.network

import com.example.fitme.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import java.lang.reflect.Field

/**
 * SECURITY: Centralized provider for sensitive configurations.
 * Uses reflection-safe access to BuildConfig to prevent build-time crashes.
 */
object SecurityProvider {

    private fun getSafeConfig(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.fitme.BuildConfig")
            val field: Field = clazz.getField(fieldName)
            field.get(null) as String
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Returns a secured FirebaseDatabase instance based on local.properties.
     */
    fun getSecuredDatabase(): FirebaseDatabase {
        val url = getSafeConfig("FIREBASE_DATABASE_URL")
        return if (url.isNotEmpty()) {
            FirebaseDatabase.getInstance(url)
        } else {
            FirebaseDatabase.getInstance()
        }
    }
}
