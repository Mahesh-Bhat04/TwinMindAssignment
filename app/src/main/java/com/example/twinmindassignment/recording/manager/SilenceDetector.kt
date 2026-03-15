package com.example.twinmindassignment.recording.manager

import com.example.twinmindassignment.core.model.SilenceResult
import com.example.twinmindassignment.core.util.Constants
import kotlin.math.sqrt

class SilenceDetector {

    private var silenceStartMs: Long? = null

    fun analyze(buffer: ShortArray, readSize: Int, currentTimeMs: Long): SilenceResult {
        val rms = calculateRms(buffer, readSize)
        return if (rms < Constants.SILENCE_THRESHOLD_RMS) {
            if (silenceStartMs == null) silenceStartMs = currentTimeMs
            val duration = currentTimeMs - silenceStartMs!!
            if (duration >= Constants.SILENCE_WARNING_MS) {
                SilenceResult.SilenceWarning(duration)
            } else {
                SilenceResult.Silent(duration)
            }
        } else {
            silenceStartMs = null
            SilenceResult.AudioDetected(rms)
        }
    }

    private fun calculateRms(buffer: ShortArray, size: Int): Double {
        if (size == 0) return 0.0
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        return sqrt(sum / size)
    }

    fun reset() {
        silenceStartMs = null
    }
}
