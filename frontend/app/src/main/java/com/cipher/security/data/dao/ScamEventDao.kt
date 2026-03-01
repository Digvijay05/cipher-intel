package com.cipher.security.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cipher.security.data.entity.ScamEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScamEventDao {
    @Insert
    suspend fun insertEvent(event: ScamEvent)

    @Query("SELECT * FROM scam_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<ScamEvent>>

    @Query("SELECT * FROM scam_events WHERE sender = :scammerId ORDER BY timestamp DESC")
    fun getEventsForScammer(scammerId: String): Flow<List<ScamEvent>>
}
