package com.honeypot.scamguard.api

import com.honeypot.scamguard.api.model.HoneypotRequest
import com.honeypot.scamguard.api.model.HoneypotResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HoneypotApiService {
    @POST("/api/honeypot/message")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: HoneypotRequest
    ): Response<HoneypotResponse>
}
