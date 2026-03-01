package com.cipher.security.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cipher.security.config.FeatureFlagManager
import com.cipher.security.worker.SmsProcessingWorker

/**
 * DEBUG ONLY Broadcast Receiver.
 * Listens for ADB intent broadcasts to simulate incoming SMS messages natively
 * without triggering Android Telephony constraint violations.
 *
 * Safety: This class exists ONLY in src/debug/ and is excluded from release builds.
 */
class DebugSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.cipher.security.DEBUG_INJECT_SMS") {
            val sender = intent.getStringExtra("sender") ?: "+10000000000"
            val body = intent.getStringExtra("body") ?: "URGENT from IRS: Please confirm your details here http://scam.io"
            val timestamp = System.currentTimeMillis()

            Log.d("DebugSmsReceiver", "Intercepted debug SMS intent from $sender")

            // DEBUG ONLY: Ensure the kill switch is disabled so the worker can process.
            // On fresh installs the flag defaults to false (kill switch active)
            // because the backend config sync endpoint is not yet wired.
            val flags = FeatureFlagManager.getInstance(context)
            if (!flags.isEngagementEnabled) {
                Log.w("DebugSmsReceiver", "Kill switch was active. Overriding for debug injection.")
                flags.applyKillSwitch(true)
            }

            // Replicate the exact enqueue strategy of the production receiver
            val inputData = Data.Builder()
                .putString("sender", sender)
                .putString("body", body)
                .putLong("timestamp", timestamp)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d("DebugSmsReceiver", "Dispatched to SmsProcessingWorker.")
        }
    }
}
