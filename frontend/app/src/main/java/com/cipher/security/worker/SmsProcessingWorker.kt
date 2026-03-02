package com.cipher.security.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cipher.security.domain.repository.ThreatRepository
import com.cipher.security.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker that processes incoming SMS through the threat detection pipeline.
 * Runs on IO dispatcher. Safe for background execution.
 *
 * Pipeline: ThreatRepository.processIncomingSms() -> Notify if high/critical -> Trigger engagement if scam.
 */
class SmsProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SmsProcessingWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sender = inputData.getString("sender") ?: return@withContext Result.failure()
        val body = inputData.getString("body") ?: return@withContext Result.failure()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        Log.d(TAG, "Processing SMS from $sender (${body.take(40)}...)")

        return@withContext try {
            // Feature flag check: Sync configs then abort if Kill Switch is engaged remotely
            val flags = com.cipher.security.config.FeatureFlagManager.getInstance(applicationContext)
            
            // Ensure we have fresh flags before processing (if network fails, it falls back to cache safely)
            flags.refreshFlags()

            // "isEngagementEnabled" signifies active system. If false, Killswitch is hit.
            if (flags.isKillSwitchActive || !flags.isEngagementEnabled) {
                Log.w(TAG, "Backend KILL SWITCH is ACTIVE or engagement disabled. Aborting SMS processing.")
                return@withContext Result.success()
            }

            val repository = ThreatRepository(applicationContext)
            val threat = repository.processIncomingSms(sender, body, timestamp)

            if (threat == null) {
                Log.d(TAG, "Message was duplicate or rate-limited. Skipping.")
                return@withContext Result.success()
            }

            // Trigger notification for high/critical threats
            if (threat.riskLevel in listOf("medium","high", "critical")) {
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showThreatNotification(
                    sender = sender,
                    riskLevel = threat.riskLevel,
                    threatId = threat.id,
                    messagePreview = body.take(120)
                )
                Log.d(TAG, "High-risk threat notification dispatched for ID=${threat.id}")
            }

            // Phase 7: Trigger autonomous engagement for confirmed scams
            if (threat.scamDetected && threat.riskLevel in listOf("medium","high", "critical")) {
                enqueueEngagement(sender, body, timestamp)
            }

            Log.d(TAG, "Processing complete: risk=${threat.riskLevel} scam=${threat.scamDetected}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed for SMS from $sender", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Enqueue the EngagementWorker to start or continue autonomous scam engagement.
     * Uses KEEP policy so only one engagement per sender is active at a time.
     */
    private fun enqueueEngagement(sender: String, body: String, timestamp: Long) {
        val engagementData = Data.Builder()
            .putString("sender", sender)
            .putString("body", body)
            .putLong("timestamp", timestamp)
            .build()

        val engagementWork = OneTimeWorkRequestBuilder<EngagementWorker>()
            .setInputData(engagementData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "engage_$sender",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            engagementWork
        )

        Log.d(TAG, "Engagement worker enqueued for sender=$sender")
    }
}
