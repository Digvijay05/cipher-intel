package com.cipher.security.api.model

data class CipherRequest(
    val sessionId: String,
    val message: RequestMessage,
    val conversationHistory: List<RequestMessage> = emptyList(),
    val metadata: RequestMetadata
)

data class RequestMessage(
    val sender: String,
    val text: String,
    val timestamp: Long
)

data class RequestMetadata(
    val channel: String = "sms",
    val language: String = "en",
    val locale: String = "en-US"
)
