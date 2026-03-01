package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

data class CipherRequest(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("message")
    val message: Message,
    
    @SerializedName("conversation_history")
    val conversationHistory: List<Message> = emptyList(),
    
    @SerializedName("metadata")
    val metadata: Metadata? = null
)

data class Message(
    @SerializedName("sender")
    val sender: String,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("timestamp")
    val timestamp: Long
)

data class Metadata(
    @SerializedName("channel")
    val channel: String? = null,
    
    @SerializedName("language")
    val language: String? = null,
    
    @SerializedName("locale")
    val locale: String? = null
)
