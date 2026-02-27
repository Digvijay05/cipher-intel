package com.honeypot.scamguard.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scam_events",
    foreignKeys = [
        ForeignKey(
            entity = ScammerProfile::class,
            parentColumns = ["scammerId"],
            childColumns = ["scammerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scammerId")]
)
data class ScamEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scammerId: String,
    val originalMessage: String,
    val agentReply: String?,
    val timestamp: Long,
    val threatScore: Double
)
