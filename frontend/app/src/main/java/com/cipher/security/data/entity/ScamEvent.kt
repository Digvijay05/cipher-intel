package com.cipher.security.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing individual scam events linked to a scammer profile.
 * FK references ScammerProfile.sender (updated from legacy scammerId).
 */
@Entity(
    tableName = "scam_events",
    foreignKeys = [
        ForeignKey(
            entity = ScammerProfile::class,
            parentColumns = ["sender"],
            childColumns = ["sender"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sender")]
)
data class ScamEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "sender") val sender: String,
    val originalMessage: String,
    val agentReply: String?,
    val timestamp: Long,
    val threatScore: Double
)
