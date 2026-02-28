package com.cipher.security.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing individual messages within an engagement conversation.
 * Used to reconstruct conversation history for backend API calls.
 */
@Entity(
    tableName = "engagement_messages",
    foreignKeys = [
        ForeignKey(
            entity = EngagementSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class EngagementMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sessionId")
    val sessionId: String,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
