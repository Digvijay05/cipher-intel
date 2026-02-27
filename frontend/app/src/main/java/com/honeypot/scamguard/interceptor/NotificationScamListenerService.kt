package com.honeypot.scamguard.interceptor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationScamListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName

        // Only intercept SMS or Messaging Apps (e.g. WhatsApp, Telegram)
        if (!isMessagingApp(packageName)) return

        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        Log.d("ScamGuard", "Intercepted message from $title: $text")

        // 1. Check if sender in Address Book (Drop if true)
        // 2. Pass text to local regex detection
        // 3. If matched, push to API Client via Coroutine/WorkManager
    }

    private fun isMessagingApp(packageName: String): Boolean {
        val targetApps = setOf(
            "com.android.mms",                // Default SMS
            "com.google.android.apps.messaging",// Google Messages
            "com.whatsapp",                   // WhatsApp
            "org.telegram.messenger"          // Telegram
        )
        return targetApps.contains(packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification dismissal if necessary
    }
}
