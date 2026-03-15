package com.example.twinmindassignment.recording.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneCallReceiver(
    private val onCallStarted: () -> Unit,
    private val onCallEnded: () -> Unit
) : BroadcastReceiver() {

    private var wasInCall = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    if (!wasInCall) {
                        wasInCall = true
                        onCallStarted()
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (wasInCall) {
                        wasInCall = false
                        onCallEnded()
                    }
                }
            }
        }
    }
}
