package com.cipher.security.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cipher.security.MainActivity

/**
 * Manages creation of the threat notification channel and dispatching
 * high-importance alerts when scam threats are detected.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "cipher_threat_alerts"
        const val CHANNEL_NAME = "Threat Alerts"
        const val CHANNEL_DESC = "High-priority alerts for detected scam threats"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            setShowBadge(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $CHANNEL_ID")
    }

    /**
     * Dispatches a threat notification with deep link to History and action buttons.
     */
    fun showThreatNotification(
        sender: String,
        riskLevel: String,
        threatId: Long,
        messagePreview: String
    ) {
        // Check POST_NOTIFICATIONS permission on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted, skipping notification")
                return
            }
        }

        // Deep link intent → opens History screen
        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "history")
            putExtra("threat_id", threatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, threatId.toInt(), deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Safe
        val markSafeIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_MARK_SAFE"
            putExtra("threat_id", threatId)
        }
        val markSafePending = PendingIntent.getActivity(
            context, (threatId + 1000).toInt(), markSafeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val riskEmoji = when (riskLevel.lowercase()) {
            "critical" -> "🔴"
            "high" -> "🟠"
            "medium" -> "🟡"
            else -> "🟢"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$riskEmoji CIPHER: ${riskLevel.uppercase()} Risk Detected")
            .setContentText("From: $sender")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("From: $sender\n$messagePreview")
                    .setSummaryText("Threat Level: ${riskLevel.uppercase()}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Mark Safe", markSafePending)
            .build()

        NotificationManagerCompat.from(context).notify(threatId.toInt(), notification)
        Log.d(TAG, "Notification dispatched for threat $threatId ($riskLevel)")
    }
}
