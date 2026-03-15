package com.example.twinmindassignment.recording.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HeadsetReceiver(
    private val onHeadsetStateChanged: (connected: Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", -1)
            val connected = state == 1
            Log.d("HeadsetReceiver", "Headset ${if (connected) "connected" else "disconnected"}")
            onHeadsetStateChanged(connected)
        }
    }
}
