package com.cipher.security.domain.repository

import android.content.Context
import android.util.Log
import com.cipher.security.BuildConfig
import com.cipher.security.api.RetrofitClient
import com.cipher.security.api.model.CipherRequest
import com.cipher.security.data.AppDatabase
import com.cipher.security.data.dao.ThreatDao
import com.cipher.security.data.entity.ThreatEntity
import com.cipher.security.detection.LocalValidator
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID

/**
 * Orchestrates the threat processing pipeline:
 * Duplicate check → Local validation → Backend analysis → Persist → Return result
 */
class ThreatRepository(context: Context) {

    companion object {
        private const val TAG = "ThreatRepository"
    }

    private val dao: ThreatDao = AppDatabase.getDatabase(context).threatDao()
    private val api = RetrofitClient.instance

    val allThreats: Flow<List<ThreatEntity>> = dao.getAllFlow()
    val totalScamsDetected: Flow<Int> = dao.getTotalScamsDetectedFlow()

    fun getHighRiskThreats(limit: Int = 20): Flow<List<ThreatEntity>> =
        dao.getHighRiskThreatsFlow(limit)

    fun getThreatsByRisk(riskLevel: String): Flow<List<ThreatEntity>> =
        dao.getByRiskLevel(riskLevel)

    /**
     * Full pipeline: dedup → validate → escalate → persist → return entity.
     * Safe to call from IO dispatcher.
     */
    suspend fun processIncomingSms(sender: String, body: String, timestamp: Long): ThreatEntity? {
        val hash = computeHash(sender, body, timestamp)

        // Duplicate check
        if (dao.existsByHash(hash)) {
            Log.d(TAG, "Duplicate message detected, skipping. hash=$hash")
            return null
        }

        // Rate limiting: max 50 messages per minute
        val oneMinuteAgo = System.currentTimeMillis() - 60_000
        if (dao.getRecentCount(oneMinuteAgo) > 50) {
            Log.w(TAG, "Rate limit exceeded. Dropping message from $sender")
            return null
        }

        // Local validation
        val localResult = LocalValidator.validate(body)

        // Insert initial record
        val entity = ThreatEntity(
            sender = sender,
            body = body,
            timestamp = timestamp,
            localScore = localResult.score,
            messageHash = hash,
            channel = "SMS"
        )
        val rowId = dao.insert(entity)
        if (rowId == -1L) {
            Log.w(TAG, "Insert failed (likely duplicate). hash=$hash")
            return null
        }

        // Backend escalation
        if (localResult.shouldEscalate) {
            try {
                val request = CipherRequest(
                    sessionId = UUID.randomUUID().toString(),
                    message = com.cipher.security.api.model.Message(
                        sender = sender,
                        text = body,
                        timestamp = timestamp
                    ),
                    metadata = com.cipher.security.api.model.Metadata(channel = "sms")
                )
                val response = api.analyzeMessage(request)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    if (analysis != null) {
                        dao.markProcessed(
                            id = rowId,
                            backendScore = analysis.confidenceScore,
                            riskLevel = analysis.riskLevel,
                            scamDetected = analysis.scamDetected
                        )
                        Log.d(TAG, "Backend analysis complete: risk=${analysis.riskLevel} score=${analysis.confidenceScore}")
                        return entity.copy(
                            id = rowId,
                            backendScore = analysis.confidenceScore,
                            riskLevel = analysis.riskLevel,
                            scamDetected = analysis.scamDetected,
                            processed = true
                        )
                    }
                } else {
                    Log.w(TAG, "Backend returned ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backend analysis failed, using local score as fallback", e)
            }

            // Offline fallback: infer risk from local score
            val fallbackRisk = when {
                localResult.score >= 0.7 -> "high"
                localResult.score >= 0.5 -> "medium"
                else -> "low"
            }
            dao.markProcessed(
                id = rowId,
                backendScore = localResult.score,
                riskLevel = fallbackRisk,
                scamDetected = localResult.score >= 0.5
            )
            return entity.copy(
                id = rowId,
                backendScore = localResult.score,
                riskLevel = fallbackRisk,
                scamDetected = localResult.score >= 0.5,
                processed = true
            )
        }

        // Low-risk: mark as processed with local score only
        dao.markProcessed(
            id = rowId,
            backendScore = localResult.score,
            riskLevel = "none",
            scamDetected = false
        )
        return entity.copy(id = rowId, processed = true, riskLevel = "none")
    }

    private fun computeHash(sender: String, body: String, timestamp: Long): String {
        val input = "$sender|$body|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
