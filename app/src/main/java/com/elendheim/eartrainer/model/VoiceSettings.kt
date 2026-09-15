package com.elendheim.eartrainer.model

/**
 * The four knobs that decide how voice mode feels. Stored as levels rather
 * than raw numbers so the settings screen can stay in plain words, and so the
 * thresholds behind them can be retuned without migrating saved values.
 */
data class VoiceSettings(
    val sensitivity: Int = 1,
    val captureLength: Int = 1,
    val steadiness: Int = 1,
    val tolerance: Int = 1,
) {
    /** Quietest signal still treated as singing. Higher level, quieter voice. */
    val silenceRms: Float
        get() = when (sensitivity) {
            0 -> 0.020f   // ignores room noise, wants a confident voice
            2 -> 0.003f   // catches humming under your breath
            else -> 0.008f
        }

    /** How long "Lock it in" listens before answering. */
    val captureMillis: Long
        get() = when (captureLength) {
            0 -> 1000L
            2 -> 2500L
            else -> 1600L
        }

    /** Frames of history the live tuner averages over. 1 means no smoothing. */
    val smoothingFrames: Int
        get() = when (steadiness) {
            0 -> 1
            2 -> 7
            else -> 3
        }

    /** How far off the tuner still calls you in tune. */
    val inTuneCents: Int
        get() = when (tolerance) {
            0 -> 10
            2 -> 35
            else -> 20
        }

    companion object {
        val SENSITIVITY_LABELS = listOf("Low", "Normal", "High")
        val CAPTURE_LABELS = listOf("Quick", "Normal", "Relaxed")
        val STEADINESS_LABELS = listOf("Snappy", "Normal", "Smooth")
        val TOLERANCE_LABELS = listOf("Strict", "Normal", "Easy")

        fun clampLevel(value: Int): Int = value.coerceIn(0, 2)
    }
}
