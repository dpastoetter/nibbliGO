package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Centered scrollable mono text on the P1 LCD for pet LLM replies. */
@Composable
fun P1LcdDialogueOverlay(
    text: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = p1Colors()
    val scrollState = rememberScrollState()
    val displayText = if (isLoading && text.isBlank()) {
        rememberLcdLoadingDots()
    } else {
        text
    }

    LaunchedEffect(displayText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val topFraction = P1DisplaySpec.TALK_REPLY_TOP_PX.toFloat() / P1DisplaySpec.LCD_HEIGHT_PX
    val bottomFraction =
        (P1DisplaySpec.LCD_HEIGHT_PX - P1DisplaySpec.TALK_REPLY_BOTTOM_PX).toFloat() /
            P1DisplaySpec.LCD_HEIGHT_PX
    val startFraction =
        P1DisplaySpec.TALK_REPLY_LEFT_PX.toFloat() / P1DisplaySpec.LCD_WIDTH_PX
    val endFraction =
        P1DisplaySpec.TALK_REPLY_RIGHT_PX.toFloat() / P1DisplaySpec.LCD_WIDTH_PX

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fontSizePx = (maxHeight.value * 0.13f).coerceIn(11f, 14f)
        val fontSize = fontSizePx.sp
        val lineHeight = (fontSizePx * 1.25f).sp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = maxHeight * topFraction,
                    bottom = maxHeight * bottomFraction,
                    start = maxWidth * startFraction,
                    end = maxWidth * endFraction,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (displayText.isNotBlank()) {
                Text(
                    text = displayText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    color = colors.lcdPixel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun rememberLcdLoadingDots(): String {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            frame = (frame + 1) % 4
        }
    }
    return when (frame) {
        0 -> "…"
        1 -> ".  "
        2 -> ".. "
        else -> "..."
    }
}
