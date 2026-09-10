package com.elendheim.eartrainer.model

import kotlin.math.pow

/**
 * The twelve pitch classes, indexed so that `PitchClass.entries[midi % 12]` is correct.
 */
enum class PitchClass(val label: String, val isBlack: Boolean) {
    C("C", false),
    CS("C#", true),
    D("D", false),
    DS("D#", true),
    E("E", false),
    F("F", false),
    FS("F#", true),
    G("G", false),
    GS("G#", true),
    A("A", false),
    AS("A#", true),
    B("B", false),
}

/**
 * A note identified by its MIDI number. MIDI 60 is middle C.
 *
 * Octave labels follow scientific pitch notation by default (middle C = C4).
 * DAW piano rolls in the FL Studio tradition call middle C "C5" instead, so
 * display code can shift the printed octave up by one via [label].
 */
data class Note(val midi: Int) {
    val pitchClass: PitchClass get() = PitchClass.entries[Math.floorMod(midi, 12)]
    val octave: Int get() = midi / 12 - 1

    fun label(flStyleOctaves: Boolean): String =
        pitchClass.label + (octave + if (flStyleOctaves) 1 else 0)

    val frequency: Double get() = 440.0 * 2.0.pow((midi - 69) / 12.0)

    companion object {
        const val MIDDLE_C = 60

        /**
         * Distance ignoring octave, for sung answers where matching the exact
         * octave is not the point.
         */
        fun pitchClassDistanceText(guess: Int, actual: Int): String {
            val raw = Math.floorMod(guess - actual, 12)
            if (raw == 0) return "same note"
            val up = raw <= 6
            val steps = if (up) raw else 12 - raw
            val unit = if (steps == 1) "1 semitone" else "$steps semitones"
            return "$unit too " + if (up) "high" else "low"
        }

        /** Number of keys between two notes, phrased for feedback text. */
        fun distanceText(guess: Int, actual: Int): String {
            val diff = guess - actual
            val keys = if (kotlin.math.abs(diff) == 1) "1 key" else "${kotlin.math.abs(diff)} keys"
            return if (diff > 0) "$keys too high" else "$keys too low"
        }
    }
}
