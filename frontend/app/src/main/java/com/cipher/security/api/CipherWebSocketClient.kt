package com.cipher.security.api

import android.util.Log
import com.cipher.security.BuildConfig
import com.cipher.security.api.model.WebSocketEvent
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lifecycle-aware WebSocket client for the CIPHER live dashboard.
 *
 * Connects to /ws/live and emits [WebSocketEvent] via a cold [Flow].
 * Handles reconnection with bounded exponential backoff without using GlobalScope.
 * Consumer must collect within a lifecycle-scoped coroutine to prevent leaks.
 */
class CipherWebSocketClient(
    private val maxReconnectAttempts: Int = 5,
    private val initialBackoffMs: Long = 1_000L
) {
    companion object {
        private const val TAG = "CipherWebSocket"
    }

    private val gson = Gson()
    private val isConnected = AtomicBoolean(false)
    private var currentWebSocket: WebSocket? = null

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Returns a cold [Flow] of [WebSocketEvent].
     * Automatically reconnects on failure up to [maxReconnectAttempts].
     */
    fun events(): Flow<WebSocketEvent> = callbackFlow {
        var reconnectAttempt = 0

        fun connect() {
            val wsUrl = BuildConfig.BASE_URL
                .replace("https://", "wss://")
                .replace("http://", "ws://") + "ws/live"

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("x-api-key", BuildConfig.API_KEY)
                // Add JWT if needed here as well later.
                .build()

            currentWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                    isConnected.set(true)
                    reconnectAttempt = 0
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val event = gson.fromJson(text, WebSocketEvent::class.java)
                        trySend(event)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse WebSocket message: ${e.message}")
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $code $reason")
                    webSocket.close(1000, null)
                    isConnected.set(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}")
                    isConnected.set(false)
                    currentWebSocket?.cancel()

                    if (reconnectAttempt < maxReconnectAttempts) {
                        reconnectAttempt++
                        val backoff = initialBackoffMs * (1L shl (reconnectAttempt - 1))
                        Log.d(TAG, "Reconnecting in ${backoff}ms (attempt $reconnectAttempt/$maxReconnectAttempts)")
                        
                        // Launch a coroutine within the callbackFlow's scope to handle delay and reconnect
                        launch {
                            delay(backoff)
                            if (isActive) {
                                connect()
                            }
                        }
                    } else {
                        Log.e(TAG, "Max reconnect attempts reached. Giving up.")
                        close()
                    }
                }
            })
        }

        connect()

        awaitClose {
            Log.d(TAG, "Flow cancelled, closing WebSocket")
            disconnect()
        }
    }

    fun disconnect() {
        currentWebSocket?.close(1000, "Client disconnecting")
        currentWebSocket = null
        isConnected.set(false)
    }

    fun isConnected(): Boolean = isConnected.get()
}
