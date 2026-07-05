package com.elendheim.eartrainer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elendheim.eartrainer.model.Difficulty
import com.elendheim.eartrainer.model.Leveling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

data class PlayerState(
    val xp: Int = 0,
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val bestStreak: Int = 0,
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val lastDailyDay: Long = -1L,
    val lastDailyScore: Int = -1,
    val difficulty: Difficulty = Difficulty(),
    val flStyleOctaves: Boolean = false,
) {
    val level: Int get() = Leveling.levelForXp(xp)
    val accuracyPercent: Int
        get() = if (totalAnswered == 0) 0 else totalCorrect * 100 / totalAnswered

    fun hasPlayedDaily(day: Long): Boolean = lastDailyDay == day
}

class ProgressRepository(private val context: Context) {

    private object Keys {
        val XP = intPreferencesKey("xp")
        val TOTAL_CORRECT = intPreferencesKey("total_correct")
        val TOTAL_ANSWERED = intPreferencesKey("total_answered")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val BEST_DAILY_STREAK = intPreferencesKey("best_daily_streak")
        val LAST_DAILY_DAY = longPreferencesKey("last_daily_day")
        val LAST_DAILY_SCORE = intPreferencesKey("last_daily_score")
        val DIFF_LOW = intPreferencesKey("diff_low")
        val DIFF_HIGH = intPreferencesKey("diff_high")
        val DIFF_BLACK_KEYS = booleanPreferencesKey("diff_black_keys")
        val DIFF_REPLAYS = intPreferencesKey("diff_replays")
        val DIFF_REFERENCE_C = booleanPreferencesKey("diff_reference_c")
        val FL_OCTAVES = booleanPreferencesKey("fl_octaves")
    }

    val state: Flow<PlayerState> = context.dataStore.data.map { prefs ->
        PlayerState(
            xp = prefs[Keys.XP] ?: 0,
            totalCorrect = prefs[Keys.TOTAL_CORRECT] ?: 0,
            totalAnswered = prefs[Keys.TOTAL_ANSWERED] ?: 0,
            bestStreak = prefs[Keys.BEST_STREAK] ?: 0,
            dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            bestDailyStreak = prefs[Keys.BEST_DAILY_STREAK] ?: 0,
            lastDailyDay = prefs[Keys.LAST_DAILY_DAY] ?: -1L,
            lastDailyScore = prefs[Keys.LAST_DAILY_SCORE] ?: -1,
            difficulty = Difficulty(
                lowMidi = prefs[Keys.DIFF_LOW] ?: 60,
                highMidi = prefs[Keys.DIFF_HIGH] ?: 71,
                includeBlackKeys = prefs[Keys.DIFF_BLACK_KEYS] ?: false,
                maxReplays = prefs[Keys.DIFF_REPLAYS] ?: Difficulty.REPLAYS_UNLIMITED,
                referenceC = prefs[Keys.DIFF_REFERENCE_C] ?: true,
            ),
            flStyleOctaves = prefs[Keys.FL_OCTAVES] ?: false,
        )
    }

    /** Records one answered question and any XP it earned, in a single write. */
    suspend fun recordAnswer(correct: Boolean, currentStreak: Int, xpGained: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_ANSWERED] = (prefs[Keys.TOTAL_ANSWERED] ?: 0) + 1
            if (correct) {
                prefs[Keys.TOTAL_CORRECT] = (prefs[Keys.TOTAL_CORRECT] ?: 0) + 1
            }
            if (currentStreak > (prefs[Keys.BEST_STREAK] ?: 0)) {
                prefs[Keys.BEST_STREAK] = currentStreak
            }
            if (xpGained > 0) {
                prefs[Keys.XP] = (prefs[Keys.XP] ?: 0) + xpGained
            }
        }
    }

    suspend fun addXp(amount: Int) {
        if (amount <= 0) return
        context.dataStore.edit { prefs ->
            prefs[Keys.XP] = (prefs[Keys.XP] ?: 0) + amount
        }
    }

    /**
     * Marks today's challenge finished and returns the new daily streak.
     * The streak continues if the previous completed day was yesterday.
     */
    suspend fun completeDaily(day: Long, score: Int): Int {
        var newStreak = 1
        context.dataStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_DAILY_DAY] ?: -1L
            if (lastDay == day) {
                // Already recorded today; keep the existing streak.
                newStreak = prefs[Keys.DAILY_STREAK] ?: 1
                return@edit
            }
            newStreak = if (lastDay == day - 1) (prefs[Keys.DAILY_STREAK] ?: 0) + 1 else 1
            prefs[Keys.DAILY_STREAK] = newStreak
            prefs[Keys.LAST_DAILY_DAY] = day
            prefs[Keys.LAST_DAILY_SCORE] = score
            if (newStreak > (prefs[Keys.BEST_DAILY_STREAK] ?: 0)) {
                prefs[Keys.BEST_DAILY_STREAK] = newStreak
            }
        }
        return newStreak
    }

    suspend fun saveDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DIFF_LOW] = difficulty.lowMidi
            prefs[Keys.DIFF_HIGH] = difficulty.highMidi
            prefs[Keys.DIFF_BLACK_KEYS] = difficulty.includeBlackKeys
            prefs[Keys.DIFF_REPLAYS] = difficulty.maxReplays
            prefs[Keys.DIFF_REFERENCE_C] = difficulty.referenceC
        }
    }

    suspend fun setFlStyleOctaves(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FL_OCTAVES] = enabled
        }
    }
}
