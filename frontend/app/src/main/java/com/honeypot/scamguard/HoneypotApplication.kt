package com.honeypot.scamguard

import android.app.Application

/**
 * Base Application class for initialization of dependency injection, logging, and workers.
 */
class HoneypotApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Core Data layer, Network clients, Notification channels, etc.
    }
}
