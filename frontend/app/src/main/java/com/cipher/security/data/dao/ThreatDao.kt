package com.cipher.security.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cipher.security.data.entity.ThreatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(threat: ThreatEntity): Long

    @Query("SELECT * FROM threats ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats WHERE risk_level = :riskLevel ORDER BY timestamp DESC")
    fun getByRiskLevel(riskLevel: String): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats WHERE processed = 0 ORDER BY timestamp ASC")
    suspend fun getUnprocessed(): List<ThreatEntity>

    @Query("UPDATE threats SET processed = 1, backend_score = :backendScore, risk_level = :riskLevel, scam_detected = :scamDetected WHERE id = :id")
    suspend fun markProcessed(id: Long, backendScore: Double, riskLevel: String, scamDetected: Boolean)

    @Query("UPDATE threats SET notified = 1 WHERE id = :id")
    suspend fun markNotified(id: Long)

    @Query("SELECT COUNT(*) FROM threats WHERE timestamp > :since")
    suspend fun getRecentCount(since: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM threats WHERE message_hash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    @Query("SELECT COUNT(*) FROM threats WHERE scam_detected = 1")
    fun getTotalScamsDetectedFlow(): Flow<Int>

    @Query("SELECT * FROM threats WHERE risk_level IN ('high', 'critical') ORDER BY timestamp DESC LIMIT :limit")
    fun getHighRiskThreatsFlow(limit: Int = 20): Flow<List<ThreatEntity>>
}
