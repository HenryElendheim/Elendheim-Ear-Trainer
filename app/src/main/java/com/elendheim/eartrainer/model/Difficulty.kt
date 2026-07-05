package com.elendheim.eartrainer.model

import kotlin.random.Random

/**
 * Everything the player can tune about how hard a round is.
 */
data class Difficulty(
    val lowMidi: Int = 60,          // C4
    val highMidi: Int = 71,         // B4
    val includeBlackKeys: Boolean = false,
    val maxReplays: Int = REPLAYS_UNLIMITED,
    val referenceC: Boolean = true, // play middle C before each note as an anchor
) {
    /** All notes this difficulty can ask about. Never empty for valid ranges. */
    fun candidates(): List<Int> =
        (lowMidi..highMidi).filter { includeBlackKeys || !PitchClass.entries[it % 12].isBlack }

    fun randomNote(random: Random): Int = candidates().random(random)

    /** Harder settings pay more per correct answer. */
    fun xpPerCorrect(): Int {
        val span = highMidi - lowMidi + 1
        val octaves = (span + 11) / 12
        var xp = 10 + (octaves - 1) * 3
        if (includeBlackKeys) xp += 5
        if (maxReplays <= 1) xp += 3
        if (!referenceC) xp += 5
        return xp
    }

    fun summary(flStyleOctaves: Boolean): String {
        val range = "${Note(lowMidi).label(flStyleOctaves)} to ${Note(highMidi).label(flStyleOctaves)}"
        val keys = if (includeBlackKeys) "all keys" else "white keys"
        return "$range, $keys"
    }

    companion object {
        const val REPLAYS_UNLIMITED = 99
        const val RANGE_MIN = 36 // C2
        const val RANGE_MAX = 96 // C7
        const val MIN_SPAN = 11  // at least one full octave
    }
}

data class Preset(val name: String, val difficulty: Difficulty)

val DIFFICULTY_PRESETS = listOf(
    Preset("First steps", Difficulty(60, 71, includeBlackKeys = false, maxReplays = Difficulty.REPLAYS_UNLIMITED, referenceC = true)),
    Preset("Warming up", Difficulty(60, 83, includeBlackKeys = false, maxReplays = 3, referenceC = true)),
    Preset("Serious", Difficulty(48, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false)),
    Preset("Full roll", Difficulty(36, 96, includeBlackKeys = true, maxReplays = 1, referenceC = false)),
)
