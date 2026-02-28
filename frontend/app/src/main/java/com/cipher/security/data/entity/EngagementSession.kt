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
    val state: String = EngagementState.DETECTED,

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
    const val DETECTED = "DETECTED"
    const val ENGAGING = "ENGAGING"
    const val COMPLETING = "COMPLETING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val EXPIRED = "EXPIRED"
}
