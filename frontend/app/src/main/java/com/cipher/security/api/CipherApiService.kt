package com.cipher.security.api

import com.cipher.security.api.model.CipherRequest
import com.cipher.security.api.model.EngageResponse
import com.cipher.security.api.model.ProfileListResponse
import com.cipher.security.api.model.ScammerProfileResponse
import com.cipher.security.api.model.ThreatAnalysisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * CIPHER API service interface.
 * All endpoints map 1:1 to backend/app/api/routes.py.
 */
interface CipherApiService {

    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    @GET("/api/v1/feature-flags")
    suspend fun getFeatureFlags(): Response<com.cipher.security.api.model.FeatureFlagsDto>

    /**
     * POST /api/v1/engage — Process incoming message and generate agent response.
     * Returns full session state including turn number and confidence.
     */
    @POST("/api/v1/engage")
    suspend fun engage(
        @Body request: CipherRequest
    ): Response<EngageResponse>

    /**
     * POST /api/v1/analyze — Stateless scam detection for client-side risk assessment.
     */
    @POST("/api/v1/analyze")
    suspend fun analyzeMessage(
        @Body request: CipherRequest
    ): Response<ThreatAnalysisResponse>

    /**
     * GET /api/v1/engage/{session_id} — Retrieve session state.
     */
    @GET("/api/v1/engage/{session_id}")
    suspend fun getEngagement(
        @Path("session_id") sessionId: String
    ): Response<com.cipher.security.data.entity.EngagementSession> // Needs specific type mapping later if needed, but entity maps well

    /**
     * GET /api/v1/profile/{sender} — Retrieve scammer profile by sender ID.
     */
    @GET("/api/v1/profile/{sender}")
    suspend fun getProfile(
        @Path("sender") sender: String
    ): Response<ScammerProfileResponse>

    /**
     * GET /api/v1/profiles — List recent scammer profiles.
     */
    @GET("/api/v1/profiles")
    suspend fun listProfiles(
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null
    ): Response<ProfileListResponse>
}
