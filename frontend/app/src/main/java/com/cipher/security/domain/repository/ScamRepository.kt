package com.cipher.security.domain.repository

import com.cipher.security.domain.model.DashboardStats
import com.cipher.security.domain.model.IntelligenceData
import com.cipher.security.domain.model.ScamEventUi
import com.cipher.security.domain.model.ScammerProfileUi
import com.cipher.security.domain.model.ServiceStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract abstracting the data layer.
 * Implementations may back this with Room, Retrofit, or mock data.
 */
interface ScamRepository {
    fun getServiceStatus(): Flow<ServiceStatus>
    fun getDashboardStats(): Flow<DashboardStats>
    fun getRecentEvents(limit: Int = 20): Flow<List<ScamEventUi>>
    fun getAllEvents(): Flow<List<ScamEventUi>>
    fun getEventsForScammer(scammerId: String): Flow<List<ScamEventUi>>
    fun getAllProfiles(): Flow<List<ScammerProfileUi>>
    suspend fun getProfileById(scammerId: String): ScammerProfileUi?
    suspend fun getIntelligenceForScammer(scammerId: String): IntelligenceData
}
