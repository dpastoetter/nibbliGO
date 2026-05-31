package com.nibbli.nibbligo.feature.pet.ui.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class PetMinigameId {
    SNACK_DROP,
    TIDY_TAP,
}

@Composable
fun PetMinigameHub(
    dailyTargetScore: Int?,
    onSelect: (PetMinigameId) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = rememberRetroPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.lcdDark, RoundedCornerShape(0.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "ARCADE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = palette.lcdBg,
            )
        }
        Text(
            text = buildString {
                append("Pick a retro mini-game to play with nibbli.")
                dailyTargetScore?.let { append(" Daily goal varies by game.") }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = palette.lcdDark,
        )
        MinigameCard(
            palette = palette,
            title = "SNACK DROP",
            tagline = "Catch falling treats in your basket",
            glyph = "▼★",
            onClick = { onSelect(PetMinigameId.SNACK_DROP) },
        )
        MinigameCard(
            palette = palette,
            title = "TIDY TAP",
            tagline = "Whack-a-mess — clean spots before they fade",
            glyph = "▣●",
            onClick = { onSelect(PetMinigameId.TIDY_TAP) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            RetroPixelButton(
                text = "Close",
                onClick = onDismiss,
                palette = palette,
            )
        }
    }
}

@Composable
private fun MinigameCard(
    palette: RetroPalette,
    title: String,
    tagline: String,
    glyph: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(palette.lcdDark, RoundedCornerShape(0.dp))
            .border(2.dp, palette.pixel, RoundedCornerShape(0.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .heightIn(min = 56.dp)
                .background(palette.accent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .background(palette.lcdBg, RoundedCornerShape(0.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                color = palette.accent,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = palette.pixel,
                )
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = palette.lcdDark,
                )
            }
        }
    }
}
