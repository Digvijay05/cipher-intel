package com.cipher.security.api.model

import com.google.gson.annotations.SerializedName

data class FeatureFlagsDto(
    @SerializedName("engagement_enabled") val engagementEnabled: Boolean,
    @SerializedName("kill_switch") val killSwitch: Boolean
)
