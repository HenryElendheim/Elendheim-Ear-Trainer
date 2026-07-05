package com.elendheim.eartrainer.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.eartrainer.data.PlayerState
import com.elendheim.eartrainer.model.Challenges
import com.elendheim.eartrainer.model.Leveling

@Composable
fun HomeScreen(
    player: PlayerState,
    todayEpochDay: Long,
    onPlayFree: () -> Unit,
    onPlayDaily: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenDifficulty: () -> Unit,
) {
    val dailyDone = player.hasPlayedDaily(todayEpochDay)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Elendheim Ear Trainer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Hear a note. Name the key.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LevelCard(player)

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Daily challenge",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (player.dailyStreak > 0) {
                        Text(
                            "${player.dailyStreak} day streak",
                            color = Warm,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Text(
                    text = if (dailyDone) {
                        "Done for today: ${player.lastDailyScore}/10. New notes at midnight."
                    } else {
                        "Ten notes, same for the whole day. Starts easy, ends on the full roll."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onPlayDaily,
                    enabled = !dailyDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text(if (dailyDone) "Come back tomorrow" else "Play today's challenge")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Challenges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${Challenges.unlockedCount(player.level)} / ${Challenges.all.size}",
                        color = Accent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = "Set runs you unlock by leveling up. Clear them for bonus XP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenChallenges, modifier = Modifier.fillMaxWidth()) {
                    Text("Open challenges")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Free play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Endless notes at your own settings: " +
                        player.difficulty.summary(player.flStyleOctaves) + ".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onPlayFree, modifier = Modifier.fillMaxWidth()) {
                    Text("Start")
                }
                OutlinedButton(onClick = onOpenDifficulty, modifier = Modifier.fillMaxWidth()) {
                    Text("Difficulty and options")
                }
            }
        }

        StatsRow(player)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LevelCard(player: PlayerState) {
    val xpInto = Leveling.xpIntoLevel(player.xp)
    val xpNeeded = Leveling.xpForNextLevel(player.xp)
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Level ${player.level}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
                Text(
                    "$xpInto / $xpNeeded XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { if (xpNeeded == 0) 0f else xpInto.toFloat() / xpNeeded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Accent,
                trackColor = Canvas,
            )
            val nextUnlock = Challenges.nextUnlockLevel(player.level)
            Text(
                text = if (nextUnlock != null) {
                    "Next challenge at level $nextUnlock"
                } else {
                    "All challenges unlocked"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun StatsRow(player: PlayerState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("Accuracy", "${player.accuracyPercent}%", Modifier.weight(1f))
        StatTile("Best streak", "${player.bestStreak}", Modifier.weight(1f))
        StatTile("Notes heard", "${player.totalAnswered}", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
