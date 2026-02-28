package com.cipher.security.api

import com.cipher.security.api.model.CipherRequest
import com.cipher.security.api.model.CipherResponse
import com.cipher.security.api.model.ThreatAnalysisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CipherApiService {
    @POST("/api/honeypot/message")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: CipherRequest
    ): Response<CipherResponse>

    @POST("/api/honeypot/analyze")
    suspend fun analyzeMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: CipherRequest
    ): Response<ThreatAnalysisResponse>
}
