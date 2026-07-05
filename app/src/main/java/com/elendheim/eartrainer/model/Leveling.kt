package com.elendheim.eartrainer.model

/**
 * Simple additive level curve: each level costs 50 XP more than the last.
 * Level 1 -> 2 costs 100 XP, 2 -> 3 costs 150 XP, and so on.
 */
object Leveling {

    fun costForLevel(level: Int): Int = 100 + (level - 1) * 50

    fun levelForXp(xp: Int): Int {
        var level = 1
        var remaining = xp
        while (remaining >= costForLevel(level)) {
            remaining -= costForLevel(level)
            level++
        }
        return level
    }

    /** XP earned inside the current level. */
    fun xpIntoLevel(xp: Int): Int {
        var level = 1
        var remaining = xp
        while (remaining >= costForLevel(level)) {
            remaining -= costForLevel(level)
            level++
        }
        return remaining
    }

    /** Total XP needed to finish the current level. */
    fun xpForNextLevel(xp: Int): Int = costForLevel(levelForXp(xp))
}
