package com.elendheim.eartrainer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Piano-roll palette: near-black canvas, green playhead accent.
val Canvas = Color(0xFF0F1115)
val Surface = Color(0xFF171A21)
val SurfaceHigh = Color(0xFF1E232D)
val Accent = Color(0xFF4ADE80)
val AccentDim = Color(0xFF166534)
val Sky = Color(0xFF7DD3FC)
val Warm = Color(0xFFFACC15)
val Wrong = Color(0xFFF87171)
val InkOnLight = Color(0xFF10141A)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = InkOnLight,
    secondary = Sky,
    onSecondary = InkOnLight,
    tertiary = Warm,
    background = Canvas,
    onBackground = Color(0xFFE7EAEE),
    surface = Surface,
    onSurface = Color(0xFFE7EAEE),
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Color(0xFFAAB2BD),
    error = Wrong,
)

@Composable
fun EarTrainerTheme(content: @Composable () -> Unit) {
    // The app is dark on purpose: it should feel like a piano roll at night.
    isSystemInDarkTheme() // reserved for a future light theme
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
