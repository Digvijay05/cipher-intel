package com.cipher.security.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally cached scammer profiles.
 * Fields align with backend ScammerProfileResponse.
 * String-serialized JSON fields use Room TypeConverters.
 */
@Entity(tableName = "scammer_profiles")
data class ScammerProfile(
    @PrimaryKey
    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "first_seen")
    val firstSeen: String? = null,

    @ColumnInfo(name = "last_seen")
    val lastSeen: String? = null,

    @ColumnInfo(name = "total_engagements")
    val totalEngagements: Int = 0,

    @ColumnInfo(name = "total_turns")
    val totalTurns: Int = 0,

    @ColumnInfo(name = "risk_score")
    val riskScore: Double = 0.0,

    @ColumnInfo(name = "scam_categories")
    val scamCategories: List<String> = emptyList(),

    @ColumnInfo(name = "extracted_entities")
    val extractedEntities: Map<String, Any> = emptyMap(),

    @ColumnInfo(name = "tactics_observed")
    val tacticsObserved: List<String> = emptyList(),

    @ColumnInfo(name = "status")
    val status: String = "active"
)
