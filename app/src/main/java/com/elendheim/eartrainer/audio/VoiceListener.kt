package com.elendheim.eartrainer.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Streams the microphone and reports the note being sung as a fractional MIDI
 * number, or -1 while nothing pitched is coming through.
 *
 * 22 kHz is plenty for the voice and halves the work the detector does per
 * frame compared with the 44.1 kHz used for playback.
 */
class VoiceListener(private val sampleRate: Int = 22050) {

    private val detector = PitchDetector(sampleRate)
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    /** Caller must hold RECORD_AUDIO; the permission check lives in the UI. */
    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onPitch: (Float) -> Unit) {
        if (isRunning) return
        job = scope.launch(Dispatchers.Default) {
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) return@launch

            val frame = 2048
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, frame * 4),
                )
            } catch (e: SecurityException) {
                return@launch
            } catch (e: IllegalArgumentException) {
                return@launch
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return@launch
            }

            val shorts = ShortArray(frame)
            val floats = FloatArray(frame)
            try {
                record.startRecording()
                while (isActive) {
                    val read = record.read(shorts, 0, frame)
                    if (read <= 0) continue
                    for (i in 0 until read) floats[i] = shorts[i] / 32768f
                    for (i in read until frame) floats[i] = 0f
                    val hz = detector.detect(floats)
                    onPitch(if (hz > 0f) PitchDetector.midiFromFrequency(hz) else -1f)
                }
            } catch (e: IllegalStateException) {
                // Another app holds the mic; fall through and clean up.
            } finally {
                try {
                    record.stop()
                } catch (e: IllegalStateException) {
                    // Already stopped.
                }
                record.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
