package com.cipher.security.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cipher.security.data.entity.ScammerProfile
import kotlinx.coroutines.flow.Flow

/**
 * DAO for scammer profile operations.
 * Column names align with the updated ScammerProfile entity.
 */
@Dao
interface ScammerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ScammerProfile)

    @Query("SELECT * FROM scammer_profiles WHERE sender = :sender")
    suspend fun getProfileBySender(sender: String): ScammerProfile?

    @Query("SELECT * FROM scammer_profiles ORDER BY last_seen DESC")
    fun getAllProfilesFlow(): Flow<List<ScammerProfile>>

    @Query("SELECT * FROM scammer_profiles WHERE status = :status ORDER BY risk_score DESC")
    fun getProfilesByStatus(status: String): Flow<List<ScammerProfile>>

    @Query(
        "UPDATE scammer_profiles SET risk_score = :score, " +
        "total_engagements = total_engagements + 1, " +
        "last_seen = :lastSeen WHERE sender = :sender"
    )
    suspend fun updateEngagement(sender: String, score: Double, lastSeen: String)

    @Query("SELECT COUNT(*) FROM scammer_profiles")
    suspend fun getProfileCount(): Int
}
