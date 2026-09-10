package com.elendheim.eartrainer.model

enum class ChordQuality(val suffix: String, val intervals: List<Int>) {
    MAJOR("", listOf(0, 4, 7)),
    MINOR("m", listOf(0, 3, 7)),
    MAJOR7("maj7", listOf(0, 4, 7, 11)),
    MINOR7("m7", listOf(0, 3, 7, 10)),
    DOMINANT7("7", listOf(0, 4, 7, 10)),
}

data class Chord(val rootMidi: Int, val quality: ChordQuality) {
    fun notes(): List<Int> = quality.intervals.map { rootMidi + it }
    fun label(): String = Note(rootMidi).pitchClass.label + quality.suffix
}

/**
 * A chord loop written in scale degrees so it can be rendered in any key.
 * The loop is what the ear leans on: hearing a note against a progression is
 * a different skill from hearing it against a single reference C.
 */
data class Progression(
    val id: String,
    val name: String,
    /** Roman-numeral shorthand, shown to the player. */
    val figures: String,
    /** Semitones above the key root, paired with the chord quality. */
    val degrees: List<Pair<Int, ChordQuality>>,
    /** Semitones above the key root the melody note may use. */
    val scale: List<Int>,
) {
    fun chords(keyRootMidi: Int): List<Chord> =
        degrees.map { (offset, quality) -> Chord(keyRootMidi + offset, quality) }

    /** Pitch classes the melody note is drawn from, in the given key. */
    fun scalePitchClasses(keyRootMidi: Int): Set<Int> =
        scale.map { Math.floorMod(keyRootMidi + it, 12) }.toSet()
}

private val MAJOR_SCALE = listOf(0, 2, 4, 5, 7, 9, 11)
private val MINOR_SCALE = listOf(0, 2, 3, 5, 7, 8, 10)

object Progressions {

    /** Warm and unhurried: the seventh chords take the edge off. */
    val CHILL = Progression(
        id = "chill",
        name = "Chill",
        figures = "Imaj7 - vi7 - ii7 - V7",
        degrees = listOf(
            0 to ChordQuality.MAJOR7,
            9 to ChordQuality.MINOR7,
            2 to ChordQuality.MINOR7,
            7 to ChordQuality.DOMINANT7,
        ),
        scale = MAJOR_SCALE,
    )

    /** The four chords behind a great many hits. */
    val POP = Progression(
        id = "pop",
        name = "Pop",
        figures = "I - V - vi - IV",
        degrees = listOf(
            0 to ChordQuality.MAJOR,
            7 to ChordQuality.MAJOR,
            9 to ChordQuality.MINOR,
            5 to ChordQuality.MAJOR,
        ),
        scale = MAJOR_SCALE,
    )

    /** Minor key, falling shape: the sad one. */
    val SAD = Progression(
        id = "sad",
        name = "Sad",
        figures = "i - VI - III - VII",
        degrees = listOf(
            0 to ChordQuality.MINOR,
            8 to ChordQuality.MAJOR,
            3 to ChordQuality.MAJOR,
            10 to ChordQuality.MAJOR,
        ),
        scale = MINOR_SCALE,
    )
}
