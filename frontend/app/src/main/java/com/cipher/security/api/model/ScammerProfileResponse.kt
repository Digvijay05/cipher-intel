package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response payload for /api/v1/profile/{sender}.
 * Maps 1:1 to backend ScammerProfileResponse via Gson.
 */
data class ScammerProfileResponse(
    @SerializedName("sender")
    val sender: String,

    @SerializedName("first_seen")
    val firstSeen: String?,

    @SerializedName("last_seen")
    val lastSeen: String?,

    @SerializedName("total_engagements")
    val totalEngagements: Int,

    @SerializedName("total_turns")
    val totalTurns: Int,

    @SerializedName("risk_score")
    val riskScore: Double,

    @SerializedName("scam_categories")
    val scamCategories: List<String>,

    @SerializedName("extracted_entities")
    val extractedEntities: Map<String, Any>,

    @SerializedName("tactics_observed")
    val tacticsObserved: List<String>,

    @SerializedName("status")
    val status: String
)

data class ProfileListResponse(
    @SerializedName("profiles")
    val profiles: List<ScammerProfileResponse>,

    @SerializedName("total_count")
    val totalCount: Int
)
