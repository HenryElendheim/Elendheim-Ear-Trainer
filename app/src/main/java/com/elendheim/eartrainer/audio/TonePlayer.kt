package com.elendheim.eartrainer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Renders and plays synthesized piano-ish tones. No sample assets: each note
 * is additive synthesis (six decaying harmonics) with a short attack and an
 * exponential decay, cached per MIDI number after first render.
 */
class TonePlayer {

    private val sampleRate = 44100
    private val toneSeconds = 1.2
    private val cache = HashMap<Int, ShortArray>()
    private var track: AudioTrack? = null
    private val lock = Any()

    /** Gap between notes when playing a sequence, in milliseconds. */
    private val sequenceStepMillis = 1000L

    suspend fun play(midi: Int) = playSequence(listOf(midi))

    suspend fun playSequence(midis: List<Int>) = withContext(Dispatchers.Default) {
        for ((index, midi) in midis.withIndex()) {
            playOne(midi)
            if (index < midis.size - 1) delay(sequenceStepMillis)
        }
    }

    fun release() {
        synchronized(lock) {
            track?.release()
            track = null
        }
    }

    private fun playOne(midi: Int) {
        val samples = synchronized(cache) { cache.getOrPut(midi) { render(midi) } }
        synchronized(lock) {
            track?.release()
            val newTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            newTrack.write(samples, 0, samples.size)
            newTrack.play()
            track = newTrack
        }
    }

    private fun render(midi: Int): ShortArray {
        val frequency = 440.0 * 2.0.pow((midi - 69) / 12.0)
        val totalSamples = (sampleRate * toneSeconds).toInt()
        val out = ShortArray(totalSamples)
        val partialAmps = doubleArrayOf(1.0, 0.5, 0.28, 0.15, 0.07, 0.04)
        val attackSamples = (sampleRate * 0.008).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleRate * 0.05).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (k in partialAmps.indices) {
                val partialFreq = frequency * (k + 1)
                if (partialFreq > sampleRate / 2.0) break
                // Higher harmonics die out faster, which is what makes it
                // sound struck rather than like an organ.
                val partialDecay = exp(-t * (2.0 + k * 1.4))
                sample += partialAmps[k] * partialDecay * sin(2.0 * PI * partialFreq * t)
            }
            var envelope = exp(-t * 1.8)
            if (i < attackSamples) envelope *= i.toDouble() / attackSamples
            val remaining = totalSamples - i
            if (remaining < releaseSamples) envelope *= remaining.toDouble() / releaseSamples
            val value = (sample * envelope * 0.4).coerceIn(-1.0, 1.0)
            out[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
