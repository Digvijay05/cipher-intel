package com.cipher.security.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cipher.security.data.entity.ScammerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ScammerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ScammerProfile)

    @Query("SELECT * FROM scammer_profiles WHERE scammerId = :id")
    suspend fun getProfileById(id: String): ScammerProfile?

    @Query("SELECT * FROM scammer_profiles ORDER BY lastEngaged DESC")
    fun getAllProfilesFlow(): Flow<List<ScammerProfile>>

    @Query("UPDATE scammer_profiles SET confidenceScore = :score, lastEngaged = :timestamp WHERE scammerId = :id")
    suspend fun updateEngagement(id: String, score: Double, timestamp: Long)
}
