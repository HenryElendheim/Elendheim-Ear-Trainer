package com.elendheim.eartrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.eartrainer.model.Note
import com.elendheim.eartrainer.model.PitchClass

/**
 * One octave of piano keys. [baseMidi] is the MIDI number of this octave's C.
 * Keys outside [enabledRange] render dimmed and ignore taps. [highlights]
 * maps MIDI numbers to a highlight color (used for answer feedback).
 */
@Composable
fun PianoKeyboard(
    baseMidi: Int,
    enabledRange: IntRange,
    includeBlackKeys: Boolean,
    interactive: Boolean,
    highlights: Map<Int, Color>,
    flStyleOctaves: Boolean,
    onKeyTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val whiteSemitones = listOf(0, 2, 4, 5, 7, 9, 11)
    // Black key semitone paired with the white-key boundary it sits on.
    val blackKeys = listOf(1 to 1, 3 to 2, 6 to 4, 8 to 5, 10 to 6)

    BoxWithConstraints(modifier = modifier.height(200.dp)) {
        val whiteKeyWidth = maxWidth / 7
        Row(Modifier.fillMaxSize()) {
            whiteSemitones.forEach { semitone ->
                val midi = baseMidi + semitone
                val enabled = interactive && midi in enabledRange
                val highlight = highlights[midi]
                val note = Note(midi)
                Box(
                    modifier = Modifier
                        .width(whiteKeyWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .background(
                            when {
                                highlight != null -> highlight
                                midi in enabledRange -> Color(0xFFF4F6F8)
                                else -> Color(0xFF3A3F49)
                            }
                        )
                        .let { m ->
                            if (enabled) m.clickable { onKeyTap(midi) } else m
                        }
                        .semantics {
                            contentDescription = note.label(flStyleOctaves)
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = note.label(flStyleOctaves),
                        color = if (highlight != null) Color.White else InkOnLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
        if (includeBlackKeys) {
            val blackKeyWidth = whiteKeyWidth * 0.62f
            blackKeys.forEach { (semitone, boundaryIndex) ->
                val midi = baseMidi + semitone
                val enabled = interactive && midi in enabledRange
                val highlight = highlights[midi]
                val note = Note(midi)
                Box(
                    modifier = Modifier
                        .offset(x = whiteKeyWidth * boundaryIndex - blackKeyWidth / 2)
                        .width(blackKeyWidth)
                        .height(120.dp)
                        .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                        .background(
                            when {
                                highlight != null -> highlight
                                midi in enabledRange -> Color(0xFF10141A)
                                else -> Color(0xFF272C35)
                            }
                        )
                        .let { m ->
                            if (enabled) m.clickable { onKeyTap(midi) } else m
                        }
                        .semantics {
                            contentDescription = note.label(flStyleOctaves)
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = PitchClass.entries[semitone].label,
                        color = if (midi in enabledRange) Color(0xFF8A93A0) else Color(0xFF4A515C),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
        }
    }
}
