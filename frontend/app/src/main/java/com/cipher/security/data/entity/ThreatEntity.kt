package com.cipher.security.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threats",
    indices = [Index(value = ["timestamp"], orders = [Index.Order.DESC])]
)
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    @ColumnInfo(name = "local_score") val localScore: Double = 0.0,
    @ColumnInfo(name = "backend_score") val backendScore: Double? = null,
    @ColumnInfo(name = "risk_level") val riskLevel: String = "none",
    @ColumnInfo(name = "scam_detected") val scamDetected: Boolean = false,
    val processed: Boolean = false,
    val notified: Boolean = false,
    val channel: String = "SMS",
    @ColumnInfo(name = "message_hash") val messageHash: String = ""
)
