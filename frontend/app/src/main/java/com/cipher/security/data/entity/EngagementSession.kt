package com.cipher.security.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity tracking active scam engagement sessions.
 * Each unique scammer phone number maps to exactly one active session.
 * Session lifecycle: DETECTED -> ENGAGING -> COMPLETING -> COMPLETED | FAILED | EXPIRED
 */
@Entity(
    tableName = "engagements",
    indices = [
        Index(value = ["senderNumber"], unique = true),
        Index(value = ["sessionId"], unique = true),
        Index(value = ["state"])
    ]
)
data class EngagementSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sessionId")
    val sessionId: String,

    @ColumnInfo(name = "senderNumber")
    val senderNumber: String,

    @ColumnInfo(name = "state")
    val state: String = EngagementState.IDLE,

    @ColumnInfo(name = "messageCount")
    val messageCount: Int = 0,

    @ColumnInfo(name = "maxMessages")
    val maxMessages: Int = 20,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastActivityAt")
    val lastActivityAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "backendAvailable")
    val backendAvailable: Boolean = true
)

/**
 * Engagement state constants. Kept as string constants for Room compatibility.
 */
object EngagementState {
    const val IDLE = "idle"
    const val DETECTING = "detecting"
    const val ENGAGING = "engaging"
    const val COMPLETING = "completing"
    const val COMPLETED = "completed"
    const val SAFE = "safe"
    const val FAILED = "failed"
    const val EXPIRED = "expired"
}
