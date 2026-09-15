package com.elendheim.eartrainer.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.elendheim.eartrainer.model.VoiceSettings
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
    fun start(scope: CoroutineScope, settings: VoiceSettings, onPitch: (Float) -> Unit) {
        if (isRunning) return
        detector.silenceRms = settings.silenceRms
        val smoothing = settings.smoothingFrames
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
            // Rolling median of the last few frames: how hard the reading
            // fights back against a wobbling voice is the steadiness setting.
            val history = ArrayDeque<Float>()
            var misses = 0
            try {
                record.startRecording()
                while (isActive) {
                    val read = record.read(shorts, 0, frame)
                    if (read <= 0) continue
                    for (i in 0 until read) floats[i] = shorts[i] / 32768f
                    for (i in read until frame) floats[i] = 0f
                    val hz = detector.detect(floats)
                    if (hz > 0f) {
                        misses = 0
                        history.addLast(PitchDetector.midiFromFrequency(hz))
                        while (history.size > smoothing) history.removeFirst()
                        onPitch(median(history))
                    } else {
                        // One dropped frame mid-note should not blank the
                        // display; a run of them means they stopped singing.
                        misses++
                        if (misses >= MISSES_BEFORE_SILENT) {
                            history.clear()
                            onPitch(-1f)
                        }
                    }
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

    private fun median(values: Collection<Float>): Float {
        if (values.isEmpty()) return -1f
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    private companion object {
        const val MISSES_BEFORE_SILENT = 2
    }
}
