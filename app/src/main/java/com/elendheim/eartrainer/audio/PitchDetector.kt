package com.elendheim.eartrainer.audio

import kotlin.math.log2
import kotlin.math.sqrt

/**
 * YIN fundamental-frequency estimation, trimmed to the range a person can
 * actually sing so one frame stays cheap enough to run on every buffer.
 */
class PitchDetector(
    private val sampleRate: Int,
    minFrequency: Float = 65f,    // around C2
    maxFrequency: Float = 1200f,  // around D6
) {
    private val tauMin = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
    private val tauMax = (sampleRate / minFrequency).toInt()

    /** Frequency in Hz, or -1 when the frame carries no clear pitch. */
    fun detect(buffer: FloatArray): Float {
        if (buffer.size <= tauMax + MIN_WINDOW) return -1f
        if (rms(buffer) < SILENCE_RMS) return -1f

        val window = buffer.size - tauMax
        val diff = FloatArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0f
            for (i in 0 until window) {
                val d = buffer[i] - buffer[i + tau]
                sum += d * d
            }
            diff[tau] = sum
        }

        // Cumulative mean normalized difference: this is what lets a single
        // absolute threshold work across loud and quiet singing alike.
        val cmnd = FloatArray(tauMax + 1)
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1..tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running <= 0f) 1f else diff[tau] * tau / running
        }

        var chosen = -1
        var tau = tauMin
        while (tau <= tauMax) {
            if (cmnd[tau] < THRESHOLD) {
                // Walk down to the bottom of this dip before committing.
                while (tau + 1 <= tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                chosen = tau
                break
            }
            tau++
        }
        if (chosen < 0) return -1f

        val refined = interpolate(cmnd, chosen)
        if (refined <= 0f) return -1f
        return sampleRate / refined
    }

    /** Sub-sample accuracy, so the reading does not jitter between cents. */
    private fun interpolate(cmnd: FloatArray, tau: Int): Float {
        if (tau <= 0 || tau >= cmnd.size - 1) return tau.toFloat()
        val a = cmnd[tau - 1]
        val b = cmnd[tau]
        val c = cmnd[tau + 1]
        val denom = 2f * (2f * b - a - c)
        if (denom == 0f) return tau.toFloat()
        return tau + (c - a) / denom
    }

    private fun rms(buffer: FloatArray): Float {
        var sum = 0.0
        for (v in buffer) sum += (v * v).toDouble()
        return sqrt(sum / buffer.size).toFloat()
    }

    companion object {
        private const val THRESHOLD = 0.12f
        private const val SILENCE_RMS = 0.008f
        private const val MIN_WINDOW = 256

        /** Fractional MIDI number, so cents survive the conversion. */
        fun midiFromFrequency(frequency: Float): Float =
            69f + 12f * log2(frequency / 440f)
    }
}
