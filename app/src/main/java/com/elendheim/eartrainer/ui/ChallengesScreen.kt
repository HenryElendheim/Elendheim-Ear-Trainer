package com.elendheim.eartrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elendheim.eartrainer.data.PlayerState
import com.elendheim.eartrainer.model.Challenge
import com.elendheim.eartrainer.model.Challenges

@Composable
fun ChallengesScreen(
    player: PlayerState,
    onPlayChallenge: (Challenge) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Challenges",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${Challenges.unlockedCount(player.level)} of ${Challenges.all.size} unlocked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) { Text("Done") }
            }
        }

        items(Challenges.all, key = { it.id }) { challenge ->
            val index = Challenges.all.indexOfFirst { it.id == challenge.id }
            val unlocked = Challenges.isUnlocked(index, player.level)
            ChallengeCard(
                challenge = challenge,
                unlocked = unlocked,
                unlockLevel = challenge.unlockLevel(),
                best = player.bestFor(challenge.id),
                flStyleOctaves = player.flStyleOctaves,
                onPlay = { onPlayChallenge(challenge) },
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    unlocked: Boolean,
    unlockLevel: Int,
    best: Int?,
    flStyleOctaves: Boolean,
    onPlay: () -> Unit,
) {
    val cleared = best == challenge.questionCount
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) SurfaceHigh else Surface,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    challenge.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (unlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    softWrap = false,
                )
                when {
                    cleared -> Badge("Cleared", Accent)
                    best != null -> Badge("Best $best/${challenge.questionCount}", Sky)
                    !unlocked -> Badge("Level $unlockLevel", Warm)
                    else -> {}
                }
            }
            Text(
                challenge.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                challenge.difficulty.summary(flStyleOctaves) +
                    ", ${challenge.questionCount} notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unlocked) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text(if (best == null) "Play" else "Play again")
                }
            } else {
                Text(
                    "Unlocks at level $unlockLevel",
                    style = MaterialTheme.typography.labelLarge,
                    color = Warm,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
    )
}
