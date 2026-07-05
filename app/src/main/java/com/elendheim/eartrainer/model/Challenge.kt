package com.elendheim.eartrainer.model

/**
 * A fixed, named run the player unlocks by leveling up. Each challenge is a
 * short set of notes at a set difficulty, so clearing one is a concrete goal
 * rather than just more free play.
 */
data class Challenge(
    val id: String,
    val name: String,
    val blurb: String,
    val difficulty: Difficulty,
    val questionCount: Int = 10,
) {
    /** The level this challenge unlocks at. */
    fun unlockLevel(): Int = Challenges.unlockLevelForIndex(Challenges.all.indexOfFirst { it.id == id })
}

object Challenges {

    /**
     * A new challenge opens every this-many levels. Not baked into the data,
     * so the cadence and the list length can both grow later without touching
     * anything that reads these helpers.
     */
    const val LEVELS_PER_UNLOCK = 5

    val all: List<Challenge> = listOf(
        Challenge(
            "warm_up", "Warm Up",
            "One octave of white keys, with an anchor. The gentle way in.",
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = Difficulty.REPLAYS_UNLIMITED, referenceC = true),
        ),
        Challenge(
            "white_octave", "White Octave",
            "Same octave, but only three replays per note.",
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = 3, referenceC = true),
        ),
        Challenge(
            "no_anchor", "No Anchor",
            "One octave of white keys, no reference C to lean on.",
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "two_octaves", "Two Octaves",
            "White keys across C4 to B5. The range opens up.",
            Difficulty(60, 83, includeBlackKeys = false, maxReplays = 3, referenceC = true),
        ),
        Challenge(
            "sharp_start", "Sharp Start",
            "Black keys join the pool for the first time.",
            Difficulty(60, 71, includeBlackKeys = true, maxReplays = 3, referenceC = true),
        ),
        Challenge(
            "sharps_solo", "Sharps Solo",
            "All twelve keys, no anchor.",
            Difficulty(60, 71, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "low_end", "Low End",
            "White keys reaching down to C3.",
            Difficulty(48, 71, includeBlackKeys = false, maxReplays = 3, referenceC = true),
        ),
        Challenge(
            "high_end", "High End",
            "White keys climbing up to B6.",
            Difficulty(72, 95, includeBlackKeys = false, maxReplays = 3, referenceC = true),
        ),
        Challenge(
            "wide_white", "Wide White",
            "Three octaves of white keys, no anchor.",
            Difficulty(48, 83, includeBlackKeys = false, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "full_keys_mid", "Full Keys",
            "Every key across two octaves, no anchor.",
            Difficulty(60, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "one_shot", "One Shot",
            "All keys, and you hear each note only once.",
            Difficulty(60, 71, includeBlackKeys = true, maxReplays = 1, referenceC = false),
        ),
        Challenge(
            "three_octaves", "Three Octaves",
            "Every key from C3 to B5, no anchor.",
            Difficulty(48, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "deep_range", "Deep Range",
            "All keys reaching down to C2.",
            Difficulty(36, 71, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "sky_high", "Sky High",
            "All keys up in the top two octaves.",
            Difficulty(72, 96, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        ),
        Challenge(
            "full_roll", "Full Roll",
            "The whole board, C2 to C7, one hearing each. The summit.",
            Difficulty(36, 96, includeBlackKeys = true, maxReplays = 1, referenceC = false),
        ),
    )

    /** The first challenge is free; each later one costs another band of levels. */
    fun unlockLevelForIndex(index: Int): Int =
        if (index <= 0) 1 else index * LEVELS_PER_UNLOCK

    fun isUnlocked(index: Int, level: Int): Boolean = level >= unlockLevelForIndex(index)

    fun unlockedCount(level: Int): Int = all.indices.count { isUnlocked(it, level) }

    /** The level of the next locked challenge, or null once everything is open. */
    fun nextUnlockLevel(level: Int): Int? =
        all.indices.firstOrNull { !isUnlocked(it, level) }?.let { unlockLevelForIndex(it) }

    fun byId(id: String): Challenge? = all.firstOrNull { it.id == id }
}
