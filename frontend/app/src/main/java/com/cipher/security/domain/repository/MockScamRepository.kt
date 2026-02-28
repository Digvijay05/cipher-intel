package com.cipher.security.domain.repository

import com.cipher.security.domain.model.DashboardStats
import com.cipher.security.domain.model.IntelligenceData
import com.cipher.security.domain.model.ScamEventUi
import com.cipher.security.domain.model.ScammerProfileUi
import com.cipher.security.domain.model.ServiceStatus
import com.cipher.security.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock repository providing realistic sample data for development.
 * Swap with a Room-backed implementation for production.
 */
class MockScamRepository : ScamRepository {

    private val serviceStatus = MutableStateFlow(ServiceStatus.ACTIVE)

    private val mockProfiles = listOf(
        ScammerProfileUi(
            scammerId = "scammer-001",
            aliases = listOf("Income Tax Dept", "CBDT Official"),
            knownHandles = listOf("+919876543210", "+918765432109"),
            extractedUpi = listOf("refund.irs@ybl", "itdept2024@paytm"),
            tacticsDetected = listOf("URGENCY", "IMPERSONATION", "THREAT"),
            confidenceScore = 0.95,
            firstSeen = System.currentTimeMillis() - 7 * 86_400_000L,
            lastEngaged = System.currentTimeMillis() - 3_600_000L,
            totalEvents = 12,
            threatLevel = ThreatLevel.CRITICAL
        ),
        ScammerProfileUi(
            scammerId = "scammer-002",
            aliases = listOf("Customer Support", "Amazon Refund"),
            knownHandles = listOf("+917654321098"),
            extractedUpi = listOf("support.amzn@icici"),
            tacticsDetected = listOf("URGENCY", "REWARD_BAIT"),
            confidenceScore = 0.82,
            firstSeen = System.currentTimeMillis() - 3 * 86_400_000L,
            lastEngaged = System.currentTimeMillis() - 7_200_000L,
            totalEvents = 6,
            threatLevel = ThreatLevel.HIGH
        ),
        ScammerProfileUi(
            scammerId = "scammer-003",
            aliases = listOf("KYC Update"),
            knownHandles = listOf("+916543210987"),
            extractedUpi = listOf("kyc.update@axl"),
            tacticsDetected = listOf("IMPERSONATION"),
            confidenceScore = 0.58,
            firstSeen = System.currentTimeMillis() - 86_400_000L,
            lastEngaged = System.currentTimeMillis() - 14_400_000L,
            totalEvents = 3,
            threatLevel = ThreatLevel.MEDIUM
        )
    )

    private val mockEvents = listOf(
        ScamEventUi(
            id = 1, scammerId = "scammer-001", senderLabel = "Income Tax Dept",
            originalMessage = "URGENT: Your PAN card is linked to suspicious transactions. Verify immediately or face legal action. Click: bit.ly/verify-pan-now",
            agentReply = "Oh dear, this sounds very serious! I'm an elderly person living alone. Can you please guide me step by step? I don't understand technology well.",
            timestamp = System.currentTimeMillis() - 3_600_000L, threatScore = 0.95,
            threatLevel = ThreatLevel.CRITICAL, channel = "SMS"
        ),
        ScamEventUi(
            id = 2, scammerId = "scammer-001", senderLabel = "CBDT Official",
            originalMessage = "Madam, your case has been escalated. Transfer ₹5,000 processing fee to UPI: refund.irs@ybl to avoid arrest.",
            agentReply = "Oh my! Arrest? I'm so scared. Let me ask my grandson to help. What is UPI? Can I pay at the bank counter instead?",
            timestamp = System.currentTimeMillis() - 2_400_000L, threatScore = 0.97,
            threatLevel = ThreatLevel.CRITICAL, channel = "SMS"
        ),
        ScamEventUi(
            id = 3, scammerId = "scammer-002", senderLabel = "Amazon Refund",
            originalMessage = "Congratulations! You've won a ₹15,000 Amazon gift card. Claim now: bit.ly/amzn-claim. Offer expires in 1 hour!",
            agentReply = "Really? That's wonderful! My grandchildren love Amazon. How do I claim it? Do I need to share my bank details?",
            timestamp = System.currentTimeMillis() - 7_200_000L, threatScore = 0.82,
            threatLevel = ThreatLevel.HIGH, channel = "WhatsApp"
        ),
        ScamEventUi(
            id = 4, scammerId = "scammer-002", senderLabel = "Customer Support",
            originalMessage = "Yes madam, to process your gift card, please share your bank account number and IFSC code for verification.",
            agentReply = "Let me find my passbook... I think it's in the almirah. Give me 10 minutes. Is SBI okay?",
            timestamp = System.currentTimeMillis() - 5_400_000L, threatScore = 0.88,
            threatLevel = ThreatLevel.HIGH, channel = "WhatsApp"
        ),
        ScamEventUi(
            id = 5, scammerId = "scammer-003", senderLabel = "KYC Update",
            originalMessage = "Dear Customer, your bank account KYC has expired. Update immediately to avoid account suspension: link.kyc-update.in/verify",
            agentReply = null,
            timestamp = System.currentTimeMillis() - 14_400_000L, threatScore = 0.58,
            threatLevel = ThreatLevel.MEDIUM, channel = "SMS"
        )
    )

    override fun getServiceStatus(): Flow<ServiceStatus> = serviceStatus

    override fun getDashboardStats(): Flow<DashboardStats> = flowOf(
        DashboardStats(
            totalScamsDetected = 21,
            activeEngagements = 2,
            intelligenceExtracted = 14,
            threatScore = 72
        )
    )

    override fun getRecentEvents(limit: Int): Flow<List<ScamEventUi>> =
        flowOf(mockEvents.take(limit))

    override fun getAllEvents(): Flow<List<ScamEventUi>> = flowOf(mockEvents)

    override fun getEventsForScammer(scammerId: String): Flow<List<ScamEventUi>> =
        flowOf(mockEvents.filter { it.scammerId == scammerId })

    override fun getAllProfiles(): Flow<List<ScammerProfileUi>> = flowOf(mockProfiles)

    override suspend fun getProfileById(scammerId: String): ScammerProfileUi? =
        mockProfiles.find { it.scammerId == scammerId }

    override suspend fun getIntelligenceForScammer(scammerId: String): IntelligenceData {
        return when (scammerId) {
            "scammer-001" -> IntelligenceData(
                upiIds = listOf("refund.irs@ybl", "itdept2024@paytm"),
                bankAccounts = listOf("SBIN0001234 - A/C ending 7890"),
                suspiciousLinks = listOf("bit.ly/verify-pan-now", "tinyurl.com/it-refund"),
                keywords = listOf("PAN card", "arrest", "legal action", "processing fee", "CBDT"),
                phoneNumbers = listOf("+919876543210", "+918765432109")
            )
            "scammer-002" -> IntelligenceData(
                upiIds = listOf("support.amzn@icici"),
                bankAccounts = emptyList(),
                suspiciousLinks = listOf("bit.ly/amzn-claim"),
                keywords = listOf("gift card", "Amazon", "claim", "bank account", "IFSC"),
                phoneNumbers = listOf("+917654321098")
            )
            else -> IntelligenceData(
                upiIds = listOf("kyc.update@axl"),
                bankAccounts = emptyList(),
                suspiciousLinks = listOf("link.kyc-update.in/verify"),
                keywords = listOf("KYC", "account suspension", "update"),
                phoneNumbers = listOf("+916543210987")
            )
        }
    }
}
