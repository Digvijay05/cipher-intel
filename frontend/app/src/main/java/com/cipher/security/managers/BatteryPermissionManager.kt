package com.cipher.security.managers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

class BatteryPermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "BatteryPermissionMgr"
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        Log.d(TAG, "isIgnoringBatteryOptimizations: $isIgnoring for ${context.packageName}")
        return isIgnoring
    }

    @SuppressLint("BatteryLife")
    fun requestBatteryExemption() {
        if (isIgnoringBatteryOptimizations()) {
            Log.d(TAG, "Battery optimizations already ignored. No action needed.")
            return
        }

        try {
            Log.d(TAG, "Attempting ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Direct exemption request failed, falling back to general settings", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback exemption request also failed", ex)
            }
        }
    }
}
