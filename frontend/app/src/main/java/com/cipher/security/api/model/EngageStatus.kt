package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * Engagement turn response status.
 */
enum class EngageStatus(val value: String) {
    @SerializedName("continue")
    CONTINUE("continue"),
    
    @SerializedName("completed")
    COMPLETED("completed"),
    
    @SerializedName("error")
    ERROR("error"),
    
    @SerializedName("disabled")
    DISABLED("disabled")
}
