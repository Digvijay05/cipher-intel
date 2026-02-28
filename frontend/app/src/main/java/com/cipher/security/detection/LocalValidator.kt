package com.cipher.security.detection

import android.util.Log

/**
 * Lightweight local heuristic validator for incoming SMS messages.
 * Scores messages based on keyword patterns, urgency markers, financial indicators,
 * and suspicious link presence. Messages above [ESCALATION_THRESHOLD] are forwarded
 * to the backend for deep analysis.
 */
object LocalValidator {

    private const val TAG = "LocalValidator"
    private const val ESCALATION_THRESHOLD = 0.3

    data class ValidationResult(
        val score: Double,
        val shouldEscalate: Boolean,
        val matchedPatterns: List<String>
    )

    private val urgencyKeywords = listOf(
        "urgent", "immediately", "verify now", "act now", "expire",
        "suspend", "block", "deactivate", "last chance", "final warning",
        "action required", "account locked", "unauthorized"
    )

    private val financialKeywords = listOf(
        "upi", "paytm", "gpay", "phonepe", "bank", "account",
        "transfer", "payment", "refund", "credit", "debit",
        "emi", "loan", "kyc", "pan card", "aadhaar", "otp",
        "pin", "cvv", "ifsc", "neft", "rtgs", "imps"
    )

    private val impersonationKeywords = listOf(
        "rbi", "income tax", "cbdt", "customs", "police",
        "court", "sbi", "hdfc", "icici", "axis",
        "amazon", "flipkart", "fedex", "dhl"
    )

    private val suspiciousPatterns = listOf(
        Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE),       // URLs
        Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\b"""),          // Emails in SMS
        Regex("""\b\d{10,12}\b"""),                                   // Long phone numbers
        Regex("""[A-Za-z0-9]+@(?:ybl|paytm|okicici|oksbi)\b"""),     // UPI IDs
        Regex("""₹\s*[\d,]+"""),                                      // Rupee amounts
        Regex("""Rs\.?\s*[\d,]+""", RegexOption.IGNORE_CASE)          // Rs amounts
    )

    fun validate(messageBody: String): ValidationResult {
        val bodyLower = messageBody.lowercase()
        var score = 0.0
        val matched = mutableListOf<String>()

        // Urgency scoring (0.15 each, max 0.45)
        var urgencyHits = 0
        for (keyword in urgencyKeywords) {
            if (bodyLower.contains(keyword)) {
                urgencyHits++
                matched.add("urgency:$keyword")
                if (urgencyHits >= 3) break
            }
        }
        score += urgencyHits * 0.15

        // Financial indicator scoring (0.10 each, max 0.30)
        var financialHits = 0
        for (keyword in financialKeywords) {
            if (bodyLower.contains(keyword)) {
                financialHits++
                matched.add("financial:$keyword")
                if (financialHits >= 3) break
            }
        }
        score += financialHits * 0.10

        // Impersonation scoring (0.20 each, max 0.40)
        var impersonationHits = 0
        for (keyword in impersonationKeywords) {
            if (bodyLower.contains(keyword)) {
                impersonationHits++
                matched.add("impersonation:$keyword")
                if (impersonationHits >= 2) break
            }
        }
        score += impersonationHits * 0.20

        // Suspicious pattern scoring (0.10 each, max 0.30)
        var patternHits = 0
        for (pattern in suspiciousPatterns) {
            if (pattern.containsMatchIn(messageBody)) {
                patternHits++
                matched.add("pattern:${pattern.pattern.take(20)}")
                if (patternHits >= 3) break
            }
        }
        score += patternHits * 0.10

        // Cap score at 1.0
        val finalScore = score.coerceIn(0.0, 1.0)
        val shouldEscalate = finalScore >= ESCALATION_THRESHOLD

        Log.d(TAG, "Validation score=$finalScore escalate=$shouldEscalate matched=$matched")
        return ValidationResult(finalScore, shouldEscalate, matched)
    }
}
