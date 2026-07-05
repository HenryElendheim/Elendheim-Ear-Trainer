package com.elendheim.eartrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elendheim.eartrainer.model.DIFFICULTY_PRESETS
import com.elendheim.eartrainer.model.Difficulty
import com.elendheim.eartrainer.model.Note

@Composable
fun DifficultyScreen(
    initial: Difficulty,
    flStyleOctaves: Boolean,
    onSave: (Difficulty) -> Unit,
    onSetFlStyleOctaves: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var difficulty by remember { mutableStateOf(initial) }
    var sliderRange by remember {
        mutableStateOf(initial.lowMidi.toFloat()..initial.highMidi.toFloat())
    }
    var flOctaves by remember { mutableStateOf(flStyleOctaves) }

    fun update(newDifficulty: Difficulty) {
        difficulty = newDifficulty
        onSave(newDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Difficulty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        Text(
            "Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DIFFICULTY_PRESETS.forEach { preset ->
                FilterChip(
                    selected = difficulty == preset.difficulty,
                    onClick = {
                        update(preset.difficulty)
                        sliderRange = preset.difficulty.lowMidi.toFloat()..
                            preset.difficulty.highMidi.toFloat()
                    },
                    label = { Text(preset.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentDim,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            "Note range",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "${Note(sliderRange.start.toInt()).label(flOctaves)} to " +
                Note(sliderRange.endInclusive.toInt()).label(flOctaves),
            style = MaterialTheme.typography.bodyLarge,
            color = Accent,
            fontWeight = FontWeight.SemiBold,
        )
        RangeSlider(
            value = sliderRange,
            onValueChange = { range ->
                sliderRange = range.start.toInt().toFloat()..range.endInclusive.toInt().toFloat()
            },
            onValueChangeFinished = {
                var low = sliderRange.start.toInt()
                var high = sliderRange.endInclusive.toInt()
                if (high - low < Difficulty.MIN_SPAN) {
                    high = (low + Difficulty.MIN_SPAN).coerceAtMost(Difficulty.RANGE_MAX)
                    low = (high - Difficulty.MIN_SPAN).coerceAtLeast(Difficulty.RANGE_MIN)
                    sliderRange = low.toFloat()..high.toFloat()
                }
                update(difficulty.copy(lowMidi = low, highMidi = high))
            },
            valueRange = Difficulty.RANGE_MIN.toFloat()..Difficulty.RANGE_MAX.toFloat(),
        )

        ToggleRow(
            title = "Include black keys",
            subtitle = "Sharps join the pool of possible notes.",
            checked = difficulty.includeBlackKeys,
            onCheckedChange = { update(difficulty.copy(includeBlackKeys = it)) },
        )

        ToggleRow(
            title = "Reference C before each note",
            subtitle = "Plays middle C first so you have an anchor. Turn it off for more XP.",
            checked = difficulty.referenceC,
            onCheckedChange = { update(difficulty.copy(referenceC = it)) },
        )

        Text(
            "Replays per note",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, Difficulty.REPLAYS_UNLIMITED).forEach { count ->
                FilterChip(
                    selected = difficulty.maxReplays == count,
                    onClick = { update(difficulty.copy(maxReplays = count)) },
                    label = {
                        Text(if (count >= Difficulty.REPLAYS_UNLIMITED) "Unlimited" else "$count")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentDim,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ToggleRow(
            title = "FL Studio octave names",
            subtitle = "Middle C shows as C5 instead of C4.",
            checked = flOctaves,
            onCheckedChange = {
                flOctaves = it
                onSetFlStyleOctaves(it)
            },
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "These settings earn ${difficulty.xpPerCorrect()} XP per correct note, " +
                "before streak bonuses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
