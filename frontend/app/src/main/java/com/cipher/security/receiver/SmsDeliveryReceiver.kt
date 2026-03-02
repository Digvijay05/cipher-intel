package com.cipher.security.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

/**
 * BroadcastReceiver for tracking sent and delivered status of autonomous SMS replies.
 */
class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsDeliveryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sender = intent.getStringExtra("sender") ?: "unknown"
        val subId = intent.getIntExtra("subscriptionId", -1)

        when (intent.action) {
            "com.cipher.security.SMS_SENT" -> {
                when (resultCode) {
                    Activity.RESULT_OK -> Log.i(TAG, "Success: SMS SENT to $sender (SIM: $subId)")
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.e(TAG, "Failure: GENERIC_FAILURE sending to $sender (SIM: $subId)")
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Log.e(TAG, "Failure: NO_SERVICE sending to $sender (SIM: $subId)")
                    SmsManager.RESULT_ERROR_NULL_PDU -> Log.e(TAG, "Failure: NULL_PDU sending to $sender (SIM: $subId)")
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Log.e(TAG, "Failure: RADIO_OFF sending to $sender (SIM: $subId)")
                    else -> Log.e(TAG, "Failure: Unknown error code $resultCode sending to $sender")
                }
            }
            "com.cipher.security.SMS_DELIVERED" -> {
                Log.i(TAG, "Success: SMS DELIVERED to $sender (SIM: $subId)")
            }
        }
    }
}
