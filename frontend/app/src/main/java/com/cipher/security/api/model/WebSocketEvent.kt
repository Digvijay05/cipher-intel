package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

/**
 * WebSocket event received from /ws/live.
 * Maps to backend EventBus event types.
 */
data class WebSocketEvent(
    @SerializedName("event")
    val event: String,

    @SerializedName("data")
    val data: Map<String, Any> = emptyMap(),

    @SerializedName("timestamp")
    val timestamp: Long = 0L
) {
    companion object {
        const val SCAM_DETECTED = "scam.detected"
        const val ENGAGEMENT_TURN = "engagement.turn"
        const val ENGAGEMENT_COMPLETED = "engagement.completed"
        const val INTEL_EXTRACTED = "intel.extracted"
        const val SESSION_STATE_CHANGED = "session.state_changed"
        const val FEATURE_FLAG_UPDATED = "feature.flag_updated"
    }
}
