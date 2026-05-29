package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState

data class LcdPetMotion(
    val bobOffsetPx: Float = 0f,
    val swayOffsetPx: Float = 0f,
    val scale: Float = 1f,
    val scaleY: Float = 1f,
)

private data class LcdMotionProfile(
    val bobAmplitudePx: Float,
    val bobPeriodMs: Int,
    val swayAmplitudePx: Float,
    val swayPeriodMs: Int,
    val breatheScale: Float,
)

private fun lcdMotionProfile(selection: SpriteSelection, pet: PetState): LcdMotionProfile {
    if (pet.condition == PetCondition.DEAD) {
        return LcdMotionProfile(0f, 800, 0f, 600, 0f)
    }
    return when (selection.primary) {
        NibbliSpriteAtlas.Frame.PLAYFUL -> LcdMotionProfile(
            bobAmplitudePx = 5.5f,
            bobPeriodMs = 420,
            swayAmplitudePx = 4f,
            swayPeriodMs = 340,
            breatheScale = 0.06f,
        )
        NibbliSpriteAtlas.Frame.EATING_A -> LcdMotionProfile(
            bobAmplitudePx = 3.5f,
            bobPeriodMs = 340,
            swayAmplitudePx = 2.5f,
            swayPeriodMs = 280,
            breatheScale = 0.035f,
        )
        NibbliSpriteAtlas.Frame.ATTENTION -> LcdMotionProfile(
            bobAmplitudePx = 4f,
            bobPeriodMs = 380,
            swayAmplitudePx = 4.5f,
            swayPeriodMs = 320,
            breatheScale = 0.045f,
        )
        NibbliSpriteAtlas.Frame.HAPPY -> LcdMotionProfile(
            bobAmplitudePx = 4.5f,
            bobPeriodMs = 440,
            swayAmplitudePx = 3f,
            swayPeriodMs = 400,
            breatheScale = 0.05f,
        )
        NibbliSpriteAtlas.Frame.HUNGRY -> LcdMotionProfile(
            bobAmplitudePx = 2f,
            bobPeriodMs = 520,
            swayAmplitudePx = 3.5f,
            swayPeriodMs = 480,
            breatheScale = 0.02f,
        )
        NibbliSpriteAtlas.Frame.SICK -> LcdMotionProfile(
            bobAmplitudePx = 1.2f,
            bobPeriodMs = 820,
            swayAmplitudePx = 1.5f,
            swayPeriodMs = 720,
            breatheScale = 0.015f,
        )
        NibbliSpriteAtlas.Frame.SLEEPING -> LcdMotionProfile(
            bobAmplitudePx = 1f,
            bobPeriodMs = 1400,
            swayAmplitudePx = 0f,
            swayPeriodMs = 1100,
            breatheScale = 0.018f,
        )
        NibbliSpriteAtlas.Frame.EGG -> LcdMotionProfile(
            bobAmplitudePx = 2.5f,
            bobPeriodMs = 580,
            swayAmplitudePx = 2f,
            swayPeriodMs = 500,
            breatheScale = 0.03f,
        )
        NibbliSpriteAtlas.Frame.IDLE_A -> LcdMotionProfile(
            bobAmplitudePx = 3f,
            bobPeriodMs = 640,
            swayAmplitudePx = 1.5f,
            swayPeriodMs = 780,
            breatheScale = 0.035f,
        )
        else -> LcdMotionProfile(
            bobAmplitudePx = 2f,
            bobPeriodMs = 720,
            swayAmplitudePx = 1f,
            swayPeriodMs = 840,
            breatheScale = 0.025f,
        )
    }
}

private fun moodMotionMultiplier(pet: PetState): Float =
    1f + (pet.stats.mood / 100f) * 0.2f

private fun frameSquashScale(frameIndex: Int): Float =
    if (frameIndex % 2 == 1) 0.92f else 1f

@Composable
fun rememberLcdPetMotion(
    selection: SpriteSelection,
    pet: PetState,
    frameIndex: Int,
    tapBoost: Boolean = false,
): LcdPetMotion {
    if (pet.condition == PetCondition.DEAD) {
        return LcdPetMotion()
    }
    val profile = lcdMotionProfile(selection, pet)
    val moodMultiplier = moodMotionMultiplier(pet)
    val infinite = rememberInfiniteTransition(label = "lcd_motion")
    val bobFast by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(profile.bobPeriodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_bob_fast",
    )
    val bobSlow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((profile.bobPeriodMs * 1.65f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_bob_slow",
    )
    val sway by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(profile.swayPeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_sway",
    )
    val dualBob = bobFast * 0.55f + bobSlow * 0.45f
    val squash = frameSquashScale(frameIndex)
    val motionMultiplier = if (tapBoost) 2f else 1f
    val breathe = 1f + (dualBob - 0.5f) * 2f * profile.breatheScale
    return LcdPetMotion(
        bobOffsetPx = dualBob * profile.bobAmplitudePx * moodMultiplier * motionMultiplier,
        swayOffsetPx = sway * profile.swayAmplitudePx * moodMultiplier * motionMultiplier,
        scale = breathe,
        scaleY = squash,
    )
}
