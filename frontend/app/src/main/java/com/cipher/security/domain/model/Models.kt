package com.cipher.security.domain.model

/** Represents on-device service lifecycle state. */
enum class ServiceStatus { ACTIVE, PAUSED, STOPPED, ERROR }

/** Severity classification for scam threats. */
enum class ThreatLevel(val label: String, val weight: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    NONE("Safe", 0)
}

/** UI-ready representation of a scam event for display in feeds and timelines. */
data class ScamEventUi(
    val id: Int,
    val scammerId: String,
    val senderLabel: String,
    val originalMessage: String,
    val agentReply: String?,
    val timestamp: Long,
    val threatScore: Double,
    val threatLevel: ThreatLevel,
    val channel: String = "SMS"
)

/** UI-ready scammer profile for detail views. */
data class ScammerProfileUi(
    val scammerId: String,
    val aliases: List<String>,
    val knownHandles: List<String>,
    val extractedUpi: List<String>,
    val tacticsDetected: List<String>,
    val confidenceScore: Double,
    val firstSeen: Long,
    val lastEngaged: Long,
    val totalEvents: Int,
    val threatLevel: ThreatLevel
)

/** Structured intelligence extracted from scammer interactions. */
data class IntelligenceData(
    val upiIds: List<String>,
    val bankAccounts: List<String>,
    val suspiciousLinks: List<String>,
    val keywords: List<String>,
    val phoneNumbers: List<String>
)

/** Dashboard aggregate statistics. */
data class DashboardStats(
    val totalScamsDetected: Int,
    val activeEngagements: Int,
    val intelligenceExtracted: Int,
    val threatScore: Int
)
