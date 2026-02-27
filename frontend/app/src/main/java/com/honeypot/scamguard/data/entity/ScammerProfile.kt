package com.honeypot.scamguard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scammer_profiles")
data class ScammerProfile(
    @PrimaryKey val scammerId: String,
    val aliases: List<String>,
    val knownHandles: List<String>,
    val extractedUpi: List<String>,
    val tacticsDetected: List<String>,
    val confidenceScore: Double,
    val firstSeen: Long,
    val lastEngaged: Long
)
