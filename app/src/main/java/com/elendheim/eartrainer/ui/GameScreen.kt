package com.elendheim.eartrainer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.elendheim.eartrainer.game.GameViewModel
import com.elendheim.eartrainer.game.Mode
import com.elendheim.eartrainer.game.Phase
import com.elendheim.eartrainer.model.Difficulty
import com.elendheim.eartrainer.model.Note
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameScreen(viewModel: GameViewModel, onExit: () -> Unit) {
    val ui by viewModel.uiState.collectAsState()

    if (ui.phase == Phase.DONE) {
        RunSummary(
            mode = ui.mode,
            title = ui.challengeName ?: "Challenge complete",
            score = ui.correctCount,
            total = ui.questionCount,
            sessionXp = ui.sessionXp,
            bonusXp = ui.bonusXp,
            dailyStreak = ui.newDailyStreak,
            previousBest = ui.previousBest,
            leveledUpTo = ui.leveledUpTo,
            onDone = onExit,
        )
        return
    }
    if (ui.phase == Phase.IDLE) return

    val lowOctave = Note(ui.difficulty.lowMidi).octave
    val highOctave = Note(ui.difficulty.highMidi).octave
    var selectedOctave by remember(ui.questionNumber, ui.mode) {
        mutableIntStateOf((lowOctave + highOctave) / 2)
    }
    selectedOctave = selectedOctave.coerceIn(lowOctave, highOctave)

    // Jump to the answer's octave after a guess so the highlight is visible.
    LaunchedEffect(ui.phase, ui.targetMidi) {
        if (ui.phase == Phase.ANSWERED) {
            selectedOctave = Note(ui.targetMidi).octave
        }
    }

    // Voice mode needs the mic; ask only when the player actually turns it on.
    val context = LocalContext.current
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMic = granted }

    DisposableEffect(ui.voiceMode, hasMic, ui.phase) {
        if (ui.voiceMode && hasMic && ui.phase == Phase.GUESSING) {
            viewModel.startListening()
        } else {
            viewModel.stopListening()
        }
        onDispose { viewModel.stopListening() }
    }

    val highlights: Map<Int, Color> = if (ui.phase == Phase.ANSWERED) {
        buildMap {
            put(ui.targetMidi, AccentDim)
            if (!ui.wasCorrect && ui.guessMidi >= 0) put(ui.guessMidi, Wrong)
        }
    } else {
        emptyMap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onExit) { Text("End") }
            Text(
                text = when (ui.mode) {
                    Mode.DAILY -> "Daily ${ui.questionNumber}/${ui.questionCount}"
                    Mode.CHALLENGE -> "${ui.questionNumber}/${ui.questionCount}"
                    Mode.FREE -> "Free play"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                "+${ui.sessionXp} XP",
                color = Accent,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (ui.mode == Mode.FREE) {
                    if (ui.streak > 0) "Streak: ${ui.streak}" else "New run"
                } else {
                    "Score: ${ui.correctCount}/${ui.answeredCount}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (ui.streak >= 5) Warm else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ui.progressionFigures?.let { figures ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    figures,
                    style = MaterialTheme.typography.labelMedium,
                    color = Sky,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        when (ui.phase) {
            Phase.GUESSING -> if (ui.voiceMode) {
                VoicePanel(
                    hasMic = hasMic,
                    livePitchMidi = ui.livePitchMidi,
                    isCapturing = ui.isCapturing,
                    hint = ui.voiceHint,
                    anyOctave = ui.voiceAnyOctave,
                    flStyleOctaves = ui.flStyleOctaves,
                    replaysLeft = ui.replaysLeft,
                    unlimited = ui.difficulty.maxReplays >= Difficulty.REPLAYS_UNLIMITED,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onReplay = { viewModel.replay() },
                    onCapture = { viewModel.captureSungNote() },
                )
            } else {
                GuessingPanel(
                    replaysLeft = ui.replaysLeft,
                    unlimited = ui.difficulty.maxReplays >= Difficulty.REPLAYS_UNLIMITED,
                    referenceC = ui.difficulty.referenceC,
                    onReplay = { viewModel.replay() },
                )
            }
            Phase.ANSWERED -> AnswerPanel(
                targetLabel = Note(ui.targetMidi).label(ui.flStyleOctaves),
                guessLabel = Note(ui.guessMidi).label(ui.flStyleOctaves),
                wasCorrect = ui.wasCorrect,
                distance = if (ui.answeredByVoice && ui.voiceAnyOctave) {
                    Note.pitchClassDistanceText(ui.guessMidi, ui.targetMidi)
                } else {
                    Note.distanceText(ui.guessMidi, ui.targetMidi)
                },
                byVoice = ui.answeredByVoice,
                octaveShifted = ui.answeredByVoice && ui.wasCorrect &&
                    ui.guessMidi != ui.targetMidi,
                leveledUpTo = ui.leveledUpTo,
                onCompare = { viewModel.compareGuessToTarget() },
                onNext = { viewModel.next() },
            )
            else -> {}
        }

        Spacer(Modifier.weight(1f))

        // Octave picker: labels follow the player's naming preference. It
        // scrolls so a wide range never clips or wraps off the edge.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            (lowOctave..highOctave).forEach { octave ->
                val displayOctave = octave + if (ui.flStyleOctaves) 1 else 0
                FilterChip(
                    selected = selectedOctave == octave,
                    onClick = { selectedOctave = octave },
                    label = { Text("C$displayOctave") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentDim,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        PianoKeyboard(
            baseMidi = (selectedOctave + 1) * 12,
            enabledRange = ui.difficulty.lowMidi..ui.difficulty.highMidi,
            includeBlackKeys = ui.difficulty.includeBlackKeys,
            interactive = ui.phase == Phase.GUESSING,
            highlights = highlights,
            flStyleOctaves = ui.flStyleOctaves,
            onKeyTap = { midi -> viewModel.submitGuess(midi) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GuessingPanel(
    replaysLeft: Int,
    unlimited: Boolean,
    referenceC: Boolean,
    onReplay: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Which key was that?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (referenceC) {
            Text(
                "You hear middle C first, then your note.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onReplay,
            enabled = unlimited || replaysLeft > 0,
        ) {
            Text(
                when {
                    unlimited -> "Hear it again"
                    replaysLeft > 0 -> "Hear it again ($replaysLeft left)"
                    else -> "No replays left"
                }
            )
        }
    }
}

@Composable
private fun VoicePanel(
    hasMic: Boolean,
    livePitchMidi: Float,
    isCapturing: Boolean,
    hint: String?,
    anyOctave: Boolean,
    flStyleOctaves: Boolean,
    replaysLeft: Int,
    unlimited: Boolean,
    onRequestMic: () -> Unit,
    onReplay: () -> Unit,
    onCapture: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!hasMic) {
                Text(
                    "Sing the note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Voice mode needs the microphone to hear what you sing. " +
                        "Nothing is recorded or sent anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onRequestMic,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text("Allow microphone")
                }
                return@Column
            }

            // Live tuner: hunt for the note before committing to it.
            val heard = livePitchMidi > 0f
            val nearest = if (heard) livePitchMidi.roundToInt() else -1
            val cents = if (heard) ((livePitchMidi - nearest) * 100f).roundToInt() else 0
            val inTune = heard && abs(cents) <= 20

            Text(
                if (heard) Note(nearest).label(flStyleOctaves) else "--",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !heard -> MaterialTheme.colorScheme.onSurfaceVariant
                    inTune -> Accent
                    else -> Sky
                },
                maxLines = 1,
                softWrap = false,
            )
            Text(
                when {
                    !heard -> "Hum or sing and your note appears here"
                    cents == 0 -> "dead on"
                    cents > 0 -> "+$cents cents"
                    else -> "$cents cents"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )

            Button(
                onClick = onCapture,
                enabled = !isCapturing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(if (isCapturing) "Listening, hold it..." else "Lock it in")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReplay,
                    enabled = unlimited || replaysLeft > 0,
                ) {
                    Text(
                        when {
                            unlimited -> "Hear it again"
                            replaysLeft > 0 -> "Again ($replaysLeft)"
                            else -> "No replays"
                        }
                    )
                }
            }

            Text(
                hint ?: if (anyOctave) {
                    "Any octave counts, so sing it where it sits comfortably."
                } else {
                    "Sing it in the same octave you heard."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (hint != null) Warm else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AnswerPanel(
    targetLabel: String,
    guessLabel: String,
    wasCorrect: Boolean,
    distance: String,
    byVoice: Boolean,
    octaveShifted: Boolean,
    leveledUpTo: Int,
    onCompare: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (wasCorrect) {
                Text(
                    "Right: $targetLabel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
                if (octaveShifted) {
                    Text(
                        "You sang $guessLabel, same note a different octave.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    "It was $targetLabel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Wrong,
                )
                Text(
                    (if (byVoice) "You sang " else "You said ") + "$guessLabel — $distance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (leveledUpTo > 0) {
                Text(
                    "Level up: $leveledUpTo",
                    color = Warm,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!wasCorrect) {
                    OutlinedButton(onClick = onCompare) { Text("Compare") }
                }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text("Next note")
                }
            }
        }
    }
}

@Composable
private fun RunSummary(
    mode: Mode,
    title: String,
    score: Int,
    total: Int,
    sessionXp: Int,
    bonusXp: Int,
    dailyStreak: Int,
    previousBest: Int,
    leveledUpTo: Int,
    onDone: () -> Unit,
) {
    val cleared = score == total
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (mode == Mode.DAILY) "Challenge complete" else title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "$score / $total",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Accent,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "+$sessionXp XP earned, including a $bonusXp XP finish bonus.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        when (mode) {
            Mode.DAILY -> Text(
                if (dailyStreak > 1) "Daily streak: $dailyStreak days." else "Daily streak started.",
                color = Warm,
                style = MaterialTheme.typography.titleMedium,
            )
            Mode.CHALLENGE -> Text(
                when {
                    cleared && previousBest < total -> "Cleared. New best."
                    cleared -> "Cleared again."
                    score > previousBest -> "New best score."
                    else -> "Best so far: $previousBest / $total."
                },
                color = if (cleared) Accent else Warm,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Mode.FREE -> {}
        }
        if (leveledUpTo > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Level up: $leveledUpTo",
                color = Warm,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("Back to home")
        }
    }
}
