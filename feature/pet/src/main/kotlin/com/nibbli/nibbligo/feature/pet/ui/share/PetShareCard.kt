package com.nibbli.nibbligo.feature.pet.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetState

private val CardBg = Color(0xFF0D1F12)
private val LcdGreen = Color(0xFF9AE66E)
private val ShellGray = Color(0xFF3A3A42)

enum class PetShareCardKind {
    TODAY,
    EVOLUTION,
    CATCH,
    QUOTE,
}

@Composable
fun PetShareCard(
    kind: PetShareCardKind,
    pet: PetState,
    modifier: Modifier = Modifier,
    evolutionStage: LifeStage? = null,
    catchScore: Int? = null,
    catchCombo: Int? = null,
    challengeLink: String? = null,
    quoteLine: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "nibbliGO",
                color = LcdGreen.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )
            when (kind) {
                PetShareCardKind.TODAY -> TodayCardBody(pet)
                PetShareCardKind.EVOLUTION -> EvolutionCardBody(pet, evolutionStage ?: pet.stage)
                PetShareCardKind.CATCH -> CatchCardBody(
                    pet = pet,
                    score = catchScore ?: 0,
                    combo = catchCombo ?: 0,
                    challengeLink = challengeLink,
                )
                PetShareCardKind.QUOTE -> QuoteCardBody(pet, quoteLine ?: pet.dialogueLine)
            }
        }
        Text(
            text = "Raised on-device · nibbliGO",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TodayCardBody(pet: PetState) {
    Text(
        text = "My ${pet.name} today",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    LcdPreview(pet)
    StatsRow(pet)
    pet.equippedCosmetic?.let {
        Text(
            text = "Wearing ${it.name.replace('_', ' ').lowercase()}",
            color = LcdGreen,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun EvolutionCardBody(pet: PetState, stage: LifeStage) {
    Text(
        text = "${pet.name} evolved!",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Now a ${stage.name.lowercase()}",
        color = LcdGreen,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    LcdPreview(pet)
    Text(
        text = "Care score ${pet.careScore}",
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 16.sp,
    )
}

@Composable
private fun CatchCardBody(
    pet: PetState,
    score: Int,
    combo: Int,
    challengeLink: String?,
) {
    Text(
        text = "Treat Catch",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "${pet.name} scored $score!",
        color = LcdGreen,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    if (combo > 1) {
        Text(
            text = "Best combo ×$combo",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
        )
    }
    if (pet.engagement.catchHighScore > 0) {
        Text(
            text = "Personal best ${pet.engagement.catchHighScore}",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp,
        )
    }
    challengeLink?.let {
        Text(
            text = "Beat my score: $it",
            color = LcdGreen.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun QuoteCardBody(pet: PetState, line: String) {
    Text(
        text = pet.name,
        color = LcdGreen,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, ShellGray, RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2E1A), RoundedCornerShape(12.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "\"${line.take(140)}\"",
            color = LcdGreen,
            fontSize = 18.sp,
            lineHeight = 26.sp,
        )
    }
}

@Composable
private fun LcdPreview(pet: PetState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, ShellGray, RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2E1A), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stageGlyph(pet.stage),
                fontSize = 48.sp,
                color = LcdGreen,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = pet.stage.name.lowercase(),
                color = LcdGreen.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun StatsRow(pet: PetState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatChip("Mood", pet.stats.mood)
        StatChip("Care", pet.careScore)
        StatChip("Skill", pet.stats.skill)
    }
}

@Composable
private fun StatChip(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(
            text = value.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

fun stageGlyph(stage: LifeStage): String = when (stage) {
    LifeStage.EGG -> "(o)"
    LifeStage.BABY -> "^_^"
    LifeStage.CHILD -> ">w<"
    LifeStage.TEEN -> "~o~"
    LifeStage.ADULT -> "\\o/"
}

fun cosmeticLabel(cosmetic: PetCosmetic?): String? =
    cosmetic?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.titlecase() }
