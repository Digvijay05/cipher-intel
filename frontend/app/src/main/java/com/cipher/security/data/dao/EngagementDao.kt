package com.cipher.security.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cipher.security.data.entity.EngagementMessage
import com.cipher.security.data.entity.EngagementSession

/**
 * DAO for engagement session and message operations.
 * Supports session lifecycle management, message history reconstruction,
 * and safety checks (active count, expiry).
 */
@Dao
interface EngagementDao {

    // --- Session operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: EngagementSession): Long

    @Query(
        "SELECT * FROM engagements WHERE senderNumber = :number " +
        "AND state NOT IN ('completed', 'failed', 'expired') LIMIT 1"
    )
    suspend fun getActiveSessionByNumber(number: String): EngagementSession?

    @Query("SELECT * FROM engagements WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): EngagementSession?

    @Query("UPDATE engagements SET state = :state, lastActivityAt = :now WHERE id = :id")
    suspend fun updateState(id: Long, state: String, now: Long)

    @Query(
        "UPDATE engagements SET messageCount = messageCount + 1, " +
        "lastActivityAt = :now WHERE id = :id"
    )
    suspend fun incrementMessageCount(id: Long, now: Long)

    @Query(
        "SELECT COUNT(*) FROM engagements WHERE state = 'engaging'"
    )
    suspend fun getActiveEngagementCount(): Int

    @Query(
        "SELECT * FROM engagements WHERE state = 'engaging' " +
        "AND lastActivityAt < :cutoff"
    )
    suspend fun getExpiredSessions(cutoff: Long): List<EngagementSession>

    @Query("UPDATE engagements SET state = 'expired' WHERE state = 'engaging' AND lastActivityAt < :cutoff")
    suspend fun expireInactiveSessions(cutoff: Long)

    // --- Message operations ---

    @Insert
    suspend fun insertMessage(message: EngagementMessage): Long

    @Query(
        "SELECT * FROM engagement_messages WHERE sessionId = :sessionId " +
        "ORDER BY timestamp ASC"
    )
    suspend fun getMessagesForSession(sessionId: String): List<EngagementMessage>

    @Query("SELECT COUNT(*) FROM engagement_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountForSession(sessionId: String): Int

    // --- Cleanup ---

    @Query("DELETE FROM engagements WHERE createdAt < :cutoff AND state IN ('completed', 'failed', 'expired')")
    suspend fun deleteOldSessions(cutoff: Long)
}
