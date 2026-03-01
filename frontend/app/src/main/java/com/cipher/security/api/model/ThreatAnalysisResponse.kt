package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * Stateless scam detection response for client-side risk assessment.
 * Maps to backend ThreatAnalysisResponse.
 */
data class ThreatAnalysisResponse(
    @SerializedName("confidence_score")
    val confidenceScore: Double,

    @SerializedName("risk_level")
    val riskLevel: String,

    @SerializedName("scam_detected")
    val scamDetected: Boolean
)
