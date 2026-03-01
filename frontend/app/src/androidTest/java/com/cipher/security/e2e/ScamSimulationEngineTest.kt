package com.cipher.security.e2e

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cipher.security.receiver.SmsReceiver
import com.cipher.security.data.AppDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ScamSimulationEngineTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var db: AppDatabase
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
        // Dependency Injection must override BASE_URL to mockWebServer.url("/").toString()
        // In a real project using Hilt/Dagger, you'd replace the Retrofit binding here
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun simulateEndToEndScamEngagement() = runTest {
        val scammerNumber = "+15550001234"
        val scamMessage = "URGENT: Your bank account is locked. Click here: http://phish.scam"

        // 1. Enqueue Fake Feature Flag Response FIRST
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"engagement_enabled": true, "kill_switch": false}""")
        )

        // 2. Enqueue Fake Backend Response (pretending the Cloud LLM generated this) SECOND
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"reply": "Oh no, which bank?", "confidence": 0.95, "tags": ["phishing", "bank"]}""")
        )

        // 2. Simulate Incoming Intercepted SMS
        val intent = Intent("android.provider.Telephony.SMS_RECEIVED").apply {
            putExtra("pdus", arrayOf(createFakeSmsPdu(scammerNumber, scamMessage)))
            putExtra("format", "3gpp")
        }
        val receiver = SmsReceiver()
        receiver.onReceive(context, intent)

        // 3. Await Worker Execution (Synthetic delay to let WorkManager enqueue)
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosByTag("ENGAGEMENT_WORKER").get(5, TimeUnit.SECONDS)
        assertTrue("Engagement worker should be enqueued", workInfos.isNotEmpty())

        // 4. Validate Retrofit Outbound Request
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        requireNotNull(request)
        assertTrue(request.path?.contains("/api/v1/honeypot/engage") == true)
        
        // 5. Database Verification (Assert Room stored it for the Dashboard)
        val threats = db.threatDao().getAllFlow().first()
        assertEquals("Should insert exactly one threat", 1, threats.size)
        val threat = threats[0]
        assertEquals(scammerNumber, threat.sender)
        assertEquals(scamMessage, threat.body)
    }

    private fun createFakeSmsPdu(sender: String, body: String): ByteArray {
        // A minimal valid 3GPP SMS Deliver PDU for testing.
        // SMSC: 07 (length) 91 (type) 4140540510F1 (+14044550011)
        // PDU Type: 04 (Deliver)
        // Sender length: 0B (11 digits)
        // Sender type: 91 (International)
        // Sender: 5155001032F4 (+15550001234)
        // Protocol ID: 00
        // Data Encoding: 00 (7-bit)
        // Timestamp: 99309251619580 (YYMMDDHHMMSS+TZ)
        // User Data Length: 04
        // User Data: F4F29C0E (Test)
        // 
        // This minimal valid structure prevents NullPointerException when parsing
        // getOriginatingAddress() from the SmsMessage object in the receiver.
        val hexPdu = "07914140540510F1040B915155001032F400009930925161958004F4F29C0E"
        return hexPdu.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
