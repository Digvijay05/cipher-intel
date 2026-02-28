package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

data class ThreatAnalysisResponse(
    @SerializedName("scamDetected") val scamDetected: Boolean,
    @SerializedName("confidenceScore") val confidenceScore: Double,
    @SerializedName("riskLevel") val riskLevel: String
)
