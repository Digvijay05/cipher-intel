package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * Session state machine states per CDB § 4.
 */
enum class SessionStatus(val value: String) {
    @SerializedName("idle")
    IDLE("idle"),
    
    @SerializedName("detecting")
    DETECTING("detecting"),
    
    @SerializedName("engaging")
    ENGAGING("engaging"),
    
    @SerializedName("completing")
    COMPLETING("completing"),
    
    @SerializedName("completed")
    COMPLETED("completed"),
    
    @SerializedName("safe")
    SAFE("safe")
}
