package com.cipher.security.e2e

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cipher.security.receiver.SmsReceiver
import com.cipher.security.database.CipherDatabase
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
    private lateinit var db: CipherDatabase
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
        // Dependency Injection must override BASE_URL to mockWebServer.url("/").toString()
        // In a real project using Hilt/Dagger, you'd replace the Retrofit binding here
        db = CipherDatabase.getDatabase(context) 
        db.clearAllTables()
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

        // 1. Enqueue Fake Backend Response (pretending the Cloud LLM generated this)
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
        val threats = db.threatDao().getAllThreats().first()
        assertEquals("Should insert exactly one threat", 1, threats.size)
        val threat = threats[0]
        assertEquals(scammerNumber, threat.senderId)
        assertEquals("Oh no, which bank?", threat.lastAutoReply)
        assertEquals("ACTIVE", threat.sessionStatus)
    }

    private fun createFakeSmsPdu(sender: String, body: String): ByteArray {
        // Generates binary 3GPP GSM PDU array for the simulated broadcast
        // Hardcoded byte array for simplicity in this architectural demo
        return ByteArray(0) 
    }
}
