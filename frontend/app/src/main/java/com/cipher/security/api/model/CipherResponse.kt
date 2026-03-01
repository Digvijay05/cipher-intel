package com.cipher.security.api.model

data class CipherResponse(
    val status: String,
    val reply: String?,
    val session_state: String? = null,
    val turn_number: Int? = null,
    val scam_detected: Boolean? = null,
    val confidence_score: Double? = null
)
