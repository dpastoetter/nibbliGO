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
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState

/** Home LCD feels calmer when awake; sleep timing stays unchanged. */
internal object LcdAwakeMotionTuning {
    const val PERIOD_SCALE = 1.75f
    const val AMPLITUDE_SCALE = 0.88f
    const val FRAME_INTERVAL_SCALE = 1.7f
    const val MOOD_MOTION_BONUS = 0.12f
}

private fun isSleepingPet(selection: SpriteSelection, pet: PetState): Boolean =
    pet.condition == PetCondition.SLEEPING ||
        selection.primary == NibbliSpriteAtlas.Frame.SLEEPING

private fun LcdMotionProfile.scaledForAwakeHome(): LcdMotionProfile {
    if (bobPeriodMs >= 1_000) return this
    return copy(
        bobPeriodMs = (bobPeriodMs * LcdAwakeMotionTuning.PERIOD_SCALE).toInt(),
        swayPeriodMs = (swayPeriodMs * LcdAwakeMotionTuning.PERIOD_SCALE).toInt(),
        bobAmplitudePx = bobAmplitudePx * LcdAwakeMotionTuning.AMPLITUDE_SCALE,
        swayAmplitudePx = swayAmplitudePx * LcdAwakeMotionTuning.AMPLITUDE_SCALE,
        hopAmplitudePx = hopAmplitudePx * LcdAwakeMotionTuning.AMPLITUDE_SCALE,
        breatheScale = breatheScale * 0.85f,
    )
}

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
    val hopAmplitudePx: Float = 0f,
)

private fun lcdMotionProfile(selection: SpriteSelection, pet: PetState): LcdMotionProfile {
    if (pet.condition == PetCondition.DEAD) {
        return LcdMotionProfile(0f, 800, 0f, 600, 0f)
    }
    return when (selection.primary) {
        NibbliSpriteAtlas.Frame.PLAYFUL -> LcdMotionProfile(
            bobAmplitudePx = 7f,
            bobPeriodMs = 320,
            swayAmplitudePx = 5.5f,
            swayPeriodMs = 260,
            breatheScale = 0.08f,
            hopAmplitudePx = 4f,
        )
        NibbliSpriteAtlas.Frame.EATING_A -> LcdMotionProfile(
            bobAmplitudePx = 4.5f,
            bobPeriodMs = 260,
            swayAmplitudePx = 3f,
            swayPeriodMs = 220,
            breatheScale = 0.05f,
            hopAmplitudePx = 1.5f,
        )
        NibbliSpriteAtlas.Frame.ATTENTION -> LcdMotionProfile(
            bobAmplitudePx = 5f,
            bobPeriodMs = 300,
            swayAmplitudePx = 6f,
            swayPeriodMs = 240,
            breatheScale = 0.055f,
            hopAmplitudePx = 2f,
        )
        NibbliSpriteAtlas.Frame.HAPPY -> LcdMotionProfile(
            bobAmplitudePx = 6f,
            bobPeriodMs = 360,
            swayAmplitudePx = 4f,
            swayPeriodMs = 320,
            breatheScale = 0.065f,
            hopAmplitudePx = 3f,
        )
        NibbliSpriteAtlas.Frame.HUNGRY -> LcdMotionProfile(
            bobAmplitudePx = 3f,
            bobPeriodMs = 440,
            swayAmplitudePx = 5f,
            swayPeriodMs = 380,
            breatheScale = 0.03f,
        )
        NibbliSpriteAtlas.Frame.SICK -> LcdMotionProfile(
            bobAmplitudePx = 1.8f,
            bobPeriodMs = 760,
            swayAmplitudePx = 2.5f,
            swayPeriodMs = 680,
            breatheScale = 0.02f,
        )
        NibbliSpriteAtlas.Frame.SLEEPING -> LcdMotionProfile(
            bobAmplitudePx = 1.4f,
            bobPeriodMs = 1200,
            swayAmplitudePx = 0f,
            swayPeriodMs = 1100,
            breatheScale = 0.025f,
        )
        NibbliSpriteAtlas.Frame.EGG -> LcdMotionProfile(
            bobAmplitudePx = 3.5f,
            bobPeriodMs = 480,
            swayAmplitudePx = 3f,
            swayPeriodMs = 420,
            breatheScale = 0.04f,
        )
        NibbliSpriteAtlas.Frame.IDLE_A -> LcdMotionProfile(
            bobAmplitudePx = 3.5f,
            bobPeriodMs = 560,
            swayAmplitudePx = 2f,
            swayPeriodMs = 680,
            breatheScale = 0.04f,
        )
        else -> LcdMotionProfile(
            bobAmplitudePx = 2.5f,
            bobPeriodMs = 640,
            swayAmplitudePx = 1.5f,
            swayPeriodMs = 760,
            breatheScale = 0.03f,
        )
    }.let { profile ->
        if (isSleepingPet(selection, pet)) profile else profile.scaledForAwakeHome()
    }
}

private fun moodMotionMultiplier(pet: PetState): Float =
    1f + (pet.stats.mood / 100f) * LcdAwakeMotionTuning.MOOD_MOTION_BONUS

private fun actionMotionMultiplier(animation: PetAnimation): Float = when (animation) {
    PetAnimation.PLAY -> 1.5f
    PetAnimation.EVOLVE -> 1.45f
    PetAnimation.EAT -> 1.3f
    PetAnimation.HAPPY -> 1.35f
    PetAnimation.ATTENTION -> 1.2f
    else -> 1f
}

private fun frameSquashScale(frameIndex: Int, selection: SpriteSelection): Pair<Float, Float> {
    val frame = selection.frameAtIndex(frameIndex)
    return when (selection.primary) {
        NibbliSpriteAtlas.Frame.EATING_A -> when (frame) {
            NibbliSpriteAtlas.Frame.EATING_A -> 1.02f to 0.86f
            NibbliSpriteAtlas.Frame.EATING_B -> 0.98f to 1.08f
            else -> 1f to 1f
        }
        NibbliSpriteAtlas.Frame.PLAYFUL -> when (frame) {
            NibbliSpriteAtlas.Frame.PLAYFUL -> 1.06f to 0.9f
            NibbliSpriteAtlas.Frame.HAPPY -> 0.96f to 1.04f
            NibbliSpriteAtlas.Frame.IDLE_B -> 1f to 0.94f
            else -> 1f to 1f
        }
        NibbliSpriteAtlas.Frame.HAPPY -> when (frame) {
            NibbliSpriteAtlas.Frame.HAPPY -> 1.04f to 0.92f
            NibbliSpriteAtlas.Frame.PLAYFUL -> 1.02f to 0.95f
            else -> 1f to 0.96f
        }
        NibbliSpriteAtlas.Frame.ATTENTION -> when (frame) {
            NibbliSpriteAtlas.Frame.ATTENTION -> 1.05f to 0.93f
            else -> 0.98f to 1f
        }
        NibbliSpriteAtlas.Frame.HUNGRY -> if (frameIndex % 3 == 0) 1.03f to 0.94f else 1f to 1f
        NibbliSpriteAtlas.Frame.SLEEPING -> 1f to if (frameIndex % 4 == 2) 0.96f else 1f
        else -> if (frameIndex % 2 == 1) 1f to 0.93f else 1f to 1f
    }
}

@Composable
fun rememberLcdPetMotion(
    selection: SpriteSelection,
    pet: PetState,
    frameIndex: Int,
    tapBoost: Boolean = false,
    phaseOffset: Float = 0f,
    motionKey: String = "default",
): LcdPetMotion {
    if (pet.condition == PetCondition.DEAD) {
        return LcdPetMotion()
    }
    val profile = lcdMotionProfile(selection, pet)
    val moodMultiplier = moodMotionMultiplier(pet)
    val actionMultiplier = actionMotionMultiplier(pet.animation)
    val infinite = rememberInfiniteTransition(label = "lcd_motion_$motionKey")
    val bobFast by infinite.animateFloat(
        initialValue = phaseOffset,
        targetValue = 1f + phaseOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(profile.bobPeriodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_bob_fast_$motionKey",
    )
    val bobSlow by infinite.animateFloat(
        initialValue = phaseOffset * 0.7f,
        targetValue = 1f + phaseOffset * 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween((profile.bobPeriodMs * 1.65f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_bob_slow_$motionKey",
    )
    val sway by infinite.animateFloat(
        initialValue = -1f + phaseOffset,
        targetValue = 1f + phaseOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(profile.swayPeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lcd_sway_$motionKey",
    )
    val hop by infinite.animateFloat(
        initialValue = phaseOffset,
        targetValue = 1f + phaseOffset,
        animationSpec = infiniteRepeatable(
            animation = tween((profile.bobPeriodMs * 0.75f).toInt().coerceAtLeast(280), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lcd_hop_$motionKey",
    )
    val dualBob = bobFast * 0.55f + bobSlow * 0.45f
    val hopPulse = if (profile.hopAmplitudePx > 0f) {
        val spike = (1f - kotlin.math.abs(hop * 2f - 1f))
        spike * spike * profile.hopAmplitudePx
    } else {
        0f
    }
    val (scaleX, scaleY) = frameSquashScale(frameIndex, selection)
    val motionMultiplier = (if (tapBoost) 1.75f else 1f) * actionMultiplier
    val breathe = 1f + (dualBob - 0.5f) * 2f * profile.breatheScale
    return LcdPetMotion(
        bobOffsetPx = (dualBob * profile.bobAmplitudePx + hopPulse) * moodMultiplier * motionMultiplier,
        swayOffsetPx = sway * profile.swayAmplitudePx * moodMultiplier * motionMultiplier,
        scale = breathe * scaleX,
        scaleY = scaleY,
    )
}
