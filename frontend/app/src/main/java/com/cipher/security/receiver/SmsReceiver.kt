package com.cipher.security.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cipher.security.worker.SmsProcessingWorker
import java.security.MessageDigest

/**
 * BroadcastReceiver for incoming SMS messages.
 * Parses PDUs safely (API 21–34), deduplicates, and enqueues background processing.
 * No heavy work is performed inside onReceive().
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = extractMessages(intent) ?: return
        Log.d(TAG, "Received ${messages.size} SMS segments")

        // Group by sender and concatenate multi-part messages
        val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

        for ((sender, parts) in grouped) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val timestamp = parts.first().timestampMillis
            val hash = computeQuickHash(sender, body, timestamp)

            // Enqueue unique worker per message hash to prevent duplicate processing
            val inputData = Data.Builder()
                .putString("sender", sender)
                .putString("body", body)
                .putLong("timestamp", timestamp)
                .putString("hash", hash)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sms_process_$hash",
                ExistingWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Enqueued processing for SMS from $sender (hash=$hash)")
        }
    }

    @Suppress("DEPRECATION")
    private fun extractMessages(intent: Intent): Array<SmsMessage>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                val pdus = intent.extras?.get("pdus") as? Array<*> ?: return null
                pdus.mapNotNull { pdu ->
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }.toTypedArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SMS PDUs", e)
            null
        }
    }

    private fun computeQuickHash(sender: String, body: String, timestamp: Long): String {
        val input = "$sender|$body|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }
}
