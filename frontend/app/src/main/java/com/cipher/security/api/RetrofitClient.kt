package com.cipher.security.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Retrofit client with production-grade circuit breaker (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
 * and retry interceptor. Interceptor ordering: circuit breaker first, then retry.
 */
object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "http://10.0.2.2:8000"

    // Circuit breaker configuration
    private const val CB_FAILURE_THRESHOLD = 5
    private const val CB_OPEN_DURATION_MS = 30_000L

    // Retry configuration
    private const val MAX_RETRIES = 2
    private const val INITIAL_DELAY_MS = 500L

    // -------------------------------------------------------------------------
    // Circuit Breaker State Machine: CLOSED -> OPEN -> HALF_OPEN -> CLOSED
    // -------------------------------------------------------------------------
    private enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

    private val cbState = AtomicReference(CircuitState.CLOSED)
    private val cbFailureCount = AtomicInteger(0)
    private val cbOpenedAt = AtomicLong(0)

    /**
     * Circuit breaker interceptor.
     *
     * CLOSED:    All requests pass through. Failures increment counter.
     *            After [CB_FAILURE_THRESHOLD] failures, transitions to OPEN.
     * OPEN:      All requests rejected immediately with IOException.
     *            After [CB_OPEN_DURATION_MS], transitions to HALF_OPEN.
     * HALF_OPEN: One probe request is allowed through.
     *            If it succeeds -> CLOSED (counter reset).
     *            If it fails   -> OPEN (timer reset).
     */
    private val circuitBreakerInterceptor = Interceptor { chain ->
        val now = System.currentTimeMillis()
        val currentState = cbState.get()

        when (currentState) {
            CircuitState.OPEN -> {
                if (now - cbOpenedAt.get() >= CB_OPEN_DURATION_MS) {
                    // Transition to HALF_OPEN: allow one probe request
                    if (cbState.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        Log.d(TAG, "Circuit breaker: OPEN -> HALF_OPEN (probing)")
                    }
                    executeWithCircuitTracking(chain)
                } else {
                    val remainingMs = CB_OPEN_DURATION_MS - (now - cbOpenedAt.get())
                    Log.w(TAG, "Circuit breaker OPEN — rejecting request (${remainingMs}ms remaining)")
                    throw IOException("Circuit breaker is open. Backend unavailable.")
                }
            }
            CircuitState.HALF_OPEN -> {
                executeWithCircuitTracking(chain)
            }
            CircuitState.CLOSED -> {
                executeWithCircuitTracking(chain)
            }
        }
    }

    private fun executeWithCircuitTracking(chain: Interceptor.Chain): Response {
        return try {
            val response = chain.proceed(chain.request())
            if (response.isSuccessful) {
                onSuccess()
            }
            response
        } catch (e: IOException) {
            onFailure()
            throw e
        }
    }

    private fun onSuccess() {
        val previousState = cbState.getAndSet(CircuitState.CLOSED)
        cbFailureCount.set(0)
        if (previousState != CircuitState.CLOSED) {
            Log.d(TAG, "Circuit breaker: $previousState -> CLOSED (success)")
        }
    }

    private fun onFailure() {
        val count = cbFailureCount.incrementAndGet()
        val currentState = cbState.get()

        when (currentState) {
            CircuitState.HALF_OPEN -> {
                // Probe failed: back to OPEN
                cbState.set(CircuitState.OPEN)
                cbOpenedAt.set(System.currentTimeMillis())
                Log.w(TAG, "Circuit breaker: HALF_OPEN -> OPEN (probe failed)")
            }
            CircuitState.CLOSED -> {
                if (count >= CB_FAILURE_THRESHOLD) {
                    cbState.set(CircuitState.OPEN)
                    cbOpenedAt.set(System.currentTimeMillis())
                    cbFailureCount.set(0)
                    Log.e(TAG, "Circuit breaker: CLOSED -> OPEN (threshold=$CB_FAILURE_THRESHOLD reached)")
                } else {
                    Log.d(TAG, "Circuit breaker: failure $count/$CB_FAILURE_THRESHOLD")
                }
            }
            CircuitState.OPEN -> {
                // Already open, no-op
            }
        }
    }

    // -------------------------------------------------------------------------
    // Retry Interceptor (bounded exponential backoff)
    // Placed OUTSIDE circuit breaker so retries do not amplify failure count.
    // -------------------------------------------------------------------------
    private val retryInterceptor = Interceptor { chain ->
        var lastException: IOException? = null
        var result: Response? = null
        for (attempt in 0..MAX_RETRIES) {
            try {
                result = chain.proceed(chain.request())
                break
            } catch (e: IOException) {
                lastException = e
                // Do not retry if circuit breaker is open
                if (e.message?.contains("Circuit breaker") == true) {
                    throw e
                }
                if (attempt < MAX_RETRIES) {
                    val delay = INITIAL_DELAY_MS * (1L shl attempt)
                    Log.w(TAG, "Retry ${attempt + 1}/$MAX_RETRIES after ${delay}ms: ${e.message}")
                    Thread.sleep(delay)
                }
            }
        }
        result ?: throw (lastException ?: IOException("Request failed after retries"))
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // -------------------------------------------------------------------------
    // OkHttp Client: retry -> circuit breaker -> logging
    // Ordering matters: retry wraps circuit breaker so each retry is one CB count.
    // -------------------------------------------------------------------------
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(retryInterceptor)
        .addInterceptor(circuitBreakerInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val instance: CipherApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(CipherApiService::class.java)
    }
}
