package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetExpression

@Composable
fun PetSpriteAnimator(
    stage: LifeStage,
    animation: PetAnimation,
    expression: PetExpression,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "pet_idle")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val frame = when (animation) {
        PetAnimation.EAT -> if (bob > 0.5f) 1 else 0
        PetAnimation.SLEEP -> 2
        PetAnimation.SICK -> 3
        PetAnimation.HAPPY, PetAnimation.PLAY -> if (bob > 0.5f) 1 else 0
        PetAnimation.ATTENTION -> if (bob > 0.3f) 1 else 2
        PetAnimation.EVOLVE -> if (bob > 0.5f) 2 else 1
        else -> if (bob > 0.5f) 0 else 1
    }
    val palette = spritePalette(expression, stage)
    Canvas(modifier = modifier) {
        scale(scale = 4f, pivot = Offset(size.width / 2, size.height / 2)) {
            drawSprite(palette, frame, animation, stage, expression)
        }
    }
}

private data class SpritePalette(
    val body: Color,
    val belly: Color,
    val eye: Color,
    val cheek: Color,
)

private fun spritePalette(expression: PetExpression, stage: LifeStage): SpritePalette {
    val base = when (expression) {
        PetExpression.HAPPY, PetExpression.PROUD -> Color(0xFF4ECDC4)
        PetExpression.HUNGRY, PetExpression.ATTENTION -> Color(0xFFFFB347)
        PetExpression.SLEEPY -> Color(0xFFB8A9C9)
        PetExpression.SICK -> Color(0xFF95A5A6)
        PetExpression.CURIOUS -> Color(0xFFFF8A80)
        else -> Color(0xFF5BC0BE)
    }
    val scale = when (stage) {
        LifeStage.EGG -> 0.7f
        LifeStage.BABY -> 0.85f
        LifeStage.ADULT -> 1.1f
        else -> 1f
    }
    return SpritePalette(
        body = base,
        belly = base.copy(alpha = 0.7f),
        eye = Color(0xFF1E2832),
        cheek = Color(0xFFFF8A80).copy(alpha = if (expression == PetExpression.HAPPY) 0.8f else 0.3f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSprite(
    palette: SpritePalette,
    frame: Int,
    animation: PetAnimation,
    stage: LifeStage,
    expression: PetExpression,
) {
    if (stage == LifeStage.EGG) {
        drawCircle(palette.body, radius = 6f, center = Offset(8f, 9f))
        return
    }
    val bounce = if (frame == 1) -1f else 0f
    val cy = 8f + bounce
  drawCircle(palette.body, radius = 7f, center = Offset(8f, cy))
    drawCircle(palette.belly, radius = 4f, center = Offset(8f, cy + 2f))
    val eyeY = cy - 1f + if (animation == PetAnimation.SLEEP) 1f else 0f
    if (animation == PetAnimation.SLEEP) {
        drawRect(palette.eye, topLeft = Offset(5f, eyeY), size = androidx.compose.ui.geometry.Size(2f, 0.5f))
        drawRect(palette.eye, topLeft = Offset(9f, eyeY), size = androidx.compose.ui.geometry.Size(2f, 0.5f))
    } else {
        drawCircle(palette.eye, radius = 1f, center = Offset(6f, eyeY))
        drawCircle(palette.eye, radius = 1f, center = Offset(10f, eyeY))
    }
    if (expression == PetExpression.SICK) {
        drawCircle(Color(0xFF88CC88), radius = 0.8f, center = Offset(8f, cy + 3f))
    }
    drawCircle(palette.cheek, radius = 0.8f, center = Offset(4f, cy + 1f))
    drawCircle(palette.cheek, radius = 0.8f, center = Offset(12f, cy + 1f))
}
