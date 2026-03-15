package com.example.twinmindassignment.recording.manager

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import javax.inject.Inject

class AudioFocusManager @Inject constructor(
    private val audioManager: AudioManager
) {
    private var focusRequest: AudioFocusRequest? = null

    fun requestFocus(
        onFocusLost: () -> Unit,
        onFocusGainedBack: () -> Unit
    ): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFocusLost()
                    AudioManager.AUDIOFOCUS_GAIN -> onFocusGainedBack()
                    // LOSS_TRANSIENT_CAN_DUCK: continue recording (we're capturing, not playing)
                }
            }
            .build()
        focusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
