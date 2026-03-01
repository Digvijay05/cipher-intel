package com.cipher.security.worker


import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cipher.security.BuildConfig
import com.cipher.security.api.RetrofitClient
import com.cipher.security.api.model.CipherRequest
import com.cipher.security.api.model.EngageStatus

import com.cipher.security.config.FeatureFlagManager
import com.cipher.security.data.AppDatabase
import com.cipher.security.data.entity.EngagementMessage
import com.cipher.security.data.entity.EngagementSession
import com.cipher.security.data.entity.EngagementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Background worker that manages autonomous scam engagement.
 *
 * Pipeline:
 *   1. Get or create EngagementSession for the scammer number
 *   2. Enforce safety limits (max messages, rate limit, concurrent sessions)
 *   3. Persist incoming scammer message
 *   4. Build conversation history from Room
 *   5. POST to /api/Cipher/message for LLM-generated reply
 *   6. Send reply via SmsManager
 *   7. Persist agent reply
 *
 * Fail-safe: retries 3x with exponential backoff, then marks session FAILED.
 */
class EngagementWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "EngagementWorker"
        private const val MAX_CONCURRENT_SESSIONS = 5
        private const val MIN_REPLY_INTERVAL_MS = 10_000L
        private const val SESSION_EXPIRY_MS = 30 * 60 * 1000L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sender = inputData.getString("sender") ?: return@withContext Result.failure()
        val body = inputData.getString("body") ?: return@withContext Result.failure()
        val incomingTimestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.engagementDao()
        val now = System.currentTimeMillis()

        Log.d(TAG, "Processing engagement for sender=$sender")

        // Kill switch: check if engagement is enabled via FeatureFlagManager
        val flags = FeatureFlagManager.getInstance(applicationContext)
        if (!flags.isEngagementEnabled) {
            Log.d(TAG, "Engagement disabled via kill switch")
            return@withContext Result.success()
        }

        // Expire stale sessions before processing
        dao.expireInactiveSessions(now - SESSION_EXPIRY_MS)

        // Get or create session for this scammer
        var session = dao.getActiveSessionByNumber(sender)
        val isNewSession = session == null

        if (isNewSession) {
            // Enforce concurrent session cap
            val activeCount = dao.getActiveEngagementCount()
            if (activeCount >= MAX_CONCURRENT_SESSIONS) {
                Log.w(TAG, "Max concurrent sessions ($MAX_CONCURRENT_SESSIONS) reached. Skipping.")
                return@withContext Result.success()
            }

            val sessionId = "eng-${UUID.randomUUID()}"
            session = EngagementSession(
                sessionId = sessionId,
                senderNumber = sender,
                state = EngagementState.ENGAGING,
                createdAt = now,
                lastActivityAt = now
            )
            dao.insertSession(session)
            Log.d(TAG, "Created new engagement session: ${session.sessionId}")
        }

        val currentSession = session!!

        // Safety: check message cap
        if (currentSession.messageCount >= currentSession.maxMessages) {
            Log.d(TAG, "Session ${currentSession.sessionId} reached max messages. Completing.")
            dao.updateState(currentSession.id, EngagementState.COMPLETED, now)
            return@withContext Result.success()
        }

        // Safety: rate limit outgoing replies
        if (!isNewSession && (now - currentSession.lastActivityAt) < MIN_REPLY_INTERVAL_MS) {
            Log.d(TAG, "Rate limited. Retrying later.")
            return@withContext Result.retry()
        }

        // Persist incoming scammer message
        dao.insertMessage(
            EngagementMessage(
                sessionId = currentSession.sessionId,
                sender = "scammer",
                text = body,
                timestamp = incomingTimestamp
            )
        )
        dao.incrementMessageCount(currentSession.id, now)

        // Build conversation history from Room
        val historyMessages = dao.getMessagesForSession(currentSession.sessionId)
        val conversationHistory = historyMessages.dropLast(1).map { msg ->
            com.cipher.security.api.model.Message(
                sender = msg.sender,
                text = msg.text,
                timestamp = msg.timestamp
            )
        }

        // The current message (last one in history)
        val currentMessage = com.cipher.security.api.model.Message(
            sender = "scammer",
            text = body,
            timestamp = incomingTimestamp
        )

        // Build request
        val request = CipherRequest(
            sessionId = currentSession.sessionId,
            message = currentMessage,
            conversationHistory = conversationHistory,
            metadata = com.cipher.security.api.model.Metadata(channel = "sms")
        )

        return@withContext try {
            val response = RetrofitClient.instance.engage(request)

            if (!response.isSuccessful) {
                Log.w(TAG, "Backend returned ${response.code()}")
                return@withContext handleRetry("Backend error ${response.code()}")
            }

            val engageResponse = response.body()
            if (engageResponse == null) {
                Log.w(TAG, "Null response body from backend")
                return@withContext handleRetry("Null response")
            }

            // Handle kill switch from backend
            if (engageResponse.status == EngageStatus.DISABLED) {
                Log.w(TAG, "Engagement disabled by backend. Applying kill switch.")
                flags.applyKillSwitch(false)
                return@withContext Result.success()
            }

            // Handle completed session
            if (engageResponse.status == EngageStatus.COMPLETED) {
                Log.d(TAG, "Session completed by backend.")
                dao.updateState(currentSession.id, EngagementState.COMPLETED, System.currentTimeMillis())
                return@withContext Result.success()
            }

            val reply = engageResponse.reply
            if (reply.isNullOrBlank()) {
                Log.w(TAG, "Empty reply from backend")
                return@withContext handleRetry("Empty reply")
            }

            // Send SMS reply to scammer
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()

                // Split long messages into multipart
                val parts = smsManager.divideMessage(reply)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(sender, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(sender, null, reply, null, null)
                }
                Log.d(TAG, "SMS reply sent to $sender: ${reply.take(50)}...")
            } catch (e: Exception) {
                Log.e(TAG, "SmsManager.sendTextMessage failed", e)
                // Do not fail the worker — message is persisted, retry on next trigger
            }

            // Persist agent reply
            dao.insertMessage(
                EngagementMessage(
                    sessionId = currentSession.sessionId,
                    sender = "agent",
                    text = reply,
                    timestamp = System.currentTimeMillis()
                )
            )
            dao.incrementMessageCount(currentSession.id, System.currentTimeMillis())

            Log.d(TAG, "Engagement turn complete. Session=${currentSession.sessionId} count=${currentSession.messageCount + 2}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Engagement pipeline failed", e)
            handleRetry(e.message ?: "Unknown error")
        }
    }

    private fun handleRetry(reason: String): Result {
        return if (runAttemptCount < 3) {
            Log.w(TAG, "Retrying ($runAttemptCount/3): $reason")
            Result.retry()
        } else {
            Log.e(TAG, "Max retries reached. Marking session FAILED: $reason")
            Result.failure()
        }
    }
}
