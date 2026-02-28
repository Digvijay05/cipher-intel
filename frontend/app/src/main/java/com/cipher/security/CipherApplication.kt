package com.cipher.security

import android.app.Application
import com.cipher.security.data.AppDatabase
import com.cipher.security.notification.NotificationHelper

/**
 * Base Application class for initialization of core infrastructure:
 * - Room database singleton
 * - Notification channel registration
 */
class CipherApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Room database singleton
        database = AppDatabase.getDatabase(this)

        // Create notification channel on app startup
        NotificationHelper(this)
    }
}
