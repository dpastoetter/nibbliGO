package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed

private val LcdGreen = Color(0xFF9EAD86)
private val LcdDark = Color(0xFF4A5D3A)
private val BezelDark = Color(0xFF2A2A2E)
private val PixelBlack = Color(0xFF1A1A1E)
private val PixelWhite = Color(0xFFF8F8F0)
private val PixelTeal = Color(0xFF3D9A8B)
private val PixelCoral = Color(0xFFE07A5F)
private val PixelLavender = Color(0xFF9B8EC4)

@Composable
fun PixelDeviceFrame(
    roomId: String,
    stage: LifeStage,
    expression: PetExpression,
    animation: PetAnimation,
    condition: PetCondition,
    activeNeed: PetNeed,
    modifier: Modifier = Modifier,
) {
    val roomColor = when (roomId) {
        "sunset" -> Color(0xFFE8B4A0)
        "forest" -> Color(0xFFA8C69A)
        else -> LcdGreen
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(BezelDark)
            .border(4.dp, Color(0xFF3D3D42), RoundedCornerShape(24.dp))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(roomColor),
            contentAlignment = Alignment.Center,
        ) {
            PetSpriteAnimator(
                stage = stage,
                expression = expression,
                animation = animation,
                condition = condition,
                modifier = Modifier.fillMaxSize(),
            )
            if (activeNeed != PetNeed.NONE && condition != PetCondition.DEAD) {
                Text(
                    text = needIcon(activeNeed),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (stage == LifeStage.EGG) {
                Text(
                    "🥚",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
    }
}

@Composable
fun PetSpriteAnimator(
    stage: LifeStage,
    expression: PetExpression,
    animation: PetAnimation,
    condition: PetCondition,
    modifier: Modifier = Modifier,
) {
    if (stage == LifeStage.EGG) return
    val infinite = rememberInfiniteTransition(label = "pet_idle")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val bodyColor = when {
        condition == PetCondition.SICK -> PixelLavender
        expression == PetExpression.HAPPY || expression == PetExpression.PROUD -> PixelTeal
        expression == PetExpression.HUNGRY -> PixelCoral
        expression == PetExpression.SLEEPY -> PixelLavender
        else -> PixelTeal.copy(alpha = 0.9f)
    }
    Canvas(modifier = modifier) {
        val px = 8f
        val cx = size.width / 2
        val cy = size.height / 2 + bob * 4f
        val scale = size.minDimension / 160f
        drawPixelPet(cx, cy, scale, px, bodyColor, animation, expression)
    }
}

private fun DrawScope.drawPixelPet(
    cx: Float,
    cy: Float,
    scale: Float,
    px: Float,
    body: Color,
    animation: PetAnimation,
    expression: PetExpression,
) {
    fun block(x: Int, y: Int, color: Color = body) {
        drawRect(
            color = color,
            topLeft = Offset(cx + x * px * scale, cy + y * px * scale),
            size = Size(px * scale, px * scale),
        )
    }
    for (dy in -2..2) {
        for (dx in -2..2) {
            if (dx * dx + dy * dy <= 5) block(dx, dy)
        }
    }
    val eyeY = if (animation == PetAnimation.SLEEP) 0 else -1
    val eyeOpen = expression != PetExpression.SLEEPY && animation != PetAnimation.SLEEP
    if (eyeOpen) {
        block(-1, eyeY, PixelWhite)
        block(1, eyeY, PixelWhite)
        block(-1, eyeY, PixelBlack)
        block(1, eyeY, PixelBlack)
    } else {
        block(-1, 0, PixelBlack)
        block(1, 0, PixelBlack)
    }
    if (animation == PetAnimation.EAT) {
        block(0, 2, PixelCoral)
        block(-1, 2, PixelCoral)
        block(1, 2, PixelCoral)
    } else if (expression == PetExpression.HAPPY) {
        block(0, 2, PixelBlack)
    }
    if (animation == PetAnimation.ATTENTION) {
        drawCircle(
            color = PixelCoral.copy(alpha = 0.5f),
            radius = 40f * scale,
            center = Offset(cx, cy - 30 * scale),
            style = Stroke(width = 2f * scale),
        )
    }
}

private fun needIcon(need: PetNeed): String = when (need) {
    PetNeed.HUNGRY -> "🍗"
    PetNeed.DIRTY -> "🧹"
    PetNeed.TIRED -> "💤"
    PetNeed.SICK -> "💊"
    PetNeed.UNHAPPY -> "💧"
    PetNeed.LONELY -> "💬"
    else -> "❗"
}
