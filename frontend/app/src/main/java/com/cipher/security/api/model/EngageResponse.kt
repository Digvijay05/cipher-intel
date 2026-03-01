package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response payload for POST /api/v1/engage.
 * Maps 1:1 to backend EngageResponse (backend/app/models/schemas.py).
 */
data class EngageResponse(
    @SerializedName("status")
    val status: EngageStatus,

    @SerializedName("reply")
    val reply: String?,

    @SerializedName("session_state")
    val sessionState: SessionStatus,

    @SerializedName("turn_number")
    val turnNumber: Int,

    @SerializedName("scam_detected")
    val scamDetected: Boolean,

    @SerializedName("confidence_score")
    val confidenceScore: Double
)
