package com.cipher.security.config

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cipher.security.BuildConfig
import com.cipher.security.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Server-enforced feature flag manager.
 *
 * Fetches configuration from the backend on app start,
 * caches flags in [EncryptedSharedPreferences], and provides
 * synchronous read access for workers and UI.
 *
 * Kill switch: If engagement_enabled is false, all engagement
 * operations must abort immediately.
 */
class FeatureFlagManager private constructor(context: Context) {

    companion object {
        private const val TAG = "FeatureFlagManager"
        private const val PREFS_NAME = "cipher_feature_flags"
        private const val KEY_ENGAGEMENT_ENABLED = "engagement_enabled"
        private const val KEY_ML_ROUTING = "ml_routing"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"

        @Volatile
        private var INSTANCE: FeatureFlagManager? = null

        fun getInstance(context: Context): FeatureFlagManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeatureFlagManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Whether the backend kill switch is explicitly active.
     */
    val isKillSwitchActive: Boolean
        get() = prefs.getBoolean("kill_switch", false)

    /**
     * Whether the engagement engine is enabled.
     * If false, the kill switch is active and all engagement must halt.
     */
    val isEngagementEnabled: Boolean
        get() {
            // Apply safe debug override
            if (BuildConfig.DEBUG && BuildConfig.DEBUG_OVERRIDE_ENGAGEMENT) {
                Log.d(TAG, "DEBUG_OVERRIDE_ENGAGEMENT is TRUE. Forcing engagement enabled.")
                return true
            }
            // Safely default to TRUE if it has never been synced, avoiding permanent disable
            // but if kill_switch is active, engagement is explicitly false.
            return prefs.getBoolean("engagement_enabled", true) && !isKillSwitchActive
        }

    /**
     * Current ML routing strategy (e.g., "zero-shot", "fine-tuned").
     */
    val mlRouting: String
        get() = prefs.getString(KEY_ML_ROUTING, "zero-shot") ?: "zero-shot"

    /**
     * Sync feature flags from the backend.
     * To be called on app start and periodically via WorkManager.
     *
     * @return true if sync succeeded, false otherwise.
     */
    suspend fun refreshFlags(): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.instance
            val response = api.getFeatureFlags()
            
            if (response.isSuccessful) {
                val flags = response.body()
                if (flags != null) {
                    prefs.edit()
                        .putBoolean("engagement_enabled", flags.engagementEnabled)
                        .putBoolean("kill_switch", flags.killSwitch)
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .apply()
                    Log.d(TAG, "Flags synced: engagement=${flags.engagementEnabled}, killSwitch=${flags.killSwitch}")
                    return@withContext true
                }
            } else {
                Log.w(TAG, "Feature flag sync failed (Code: ${response.code()}). Using cached state.")
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Feature flag sync error (Network). Using cached state.", e)
            return@withContext false
        }
    }

    /**
     * Apply a kill switch override from a WebSocket event.
     * Used when the backend pushes a feature.flag_updated event.
     */
    fun applyKillSwitch(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ENGAGEMENT_ENABLED, enabled)
            .apply()
        Log.w(TAG, "Kill switch applied: engagement_enabled=$enabled")
    }
}
