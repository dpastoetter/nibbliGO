package com.nibbli.nibbligo.feature.pet.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class PetFeedbackKind {
    TAP,
    CARE_CONFIRM,
    QUEST_COMPLETE,
    MINIGAME_WIN,
}

class PetFeedbackController(
    private val view: View,
) {
    private var toneGenerator: ToneGenerator? = null

    fun perform(kind: PetFeedbackKind, soundEnabled: Boolean) {
        performHaptic(kind)
        if (soundEnabled) {
            performTone(kind)
        }
    }

    private fun performHaptic(kind: PetFeedbackKind) {
        val constant = when (kind) {
            PetFeedbackKind.TAP -> HapticFeedbackConstants.CLOCK_TICK
            PetFeedbackKind.CARE_CONFIRM -> HapticFeedbackConstants.CONFIRM
            PetFeedbackKind.QUEST_COMPLETE,
            PetFeedbackKind.MINIGAME_WIN,
            -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }

    private fun performTone(kind: PetFeedbackKind) {
        val tone = when (kind) {
            PetFeedbackKind.TAP -> ToneGenerator.TONE_PROP_BEEP
            PetFeedbackKind.CARE_CONFIRM -> ToneGenerator.TONE_PROP_ACK
            PetFeedbackKind.QUEST_COMPLETE -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            PetFeedbackKind.MINIGAME_WIN -> ToneGenerator.TONE_CDMA_ONE_MIN_BEEP
        }
        val duration = when (kind) {
            PetFeedbackKind.TAP -> 40
            PetFeedbackKind.CARE_CONFIRM -> 60
            PetFeedbackKind.QUEST_COMPLETE,
            PetFeedbackKind.MINIGAME_WIN,
            -> 90
        }
        runCatching {
            val generator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35).also {
                toneGenerator = it
            }
            generator.startTone(tone, duration)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}

@Composable
fun rememberPetFeedbackController(): PetFeedbackController {
    val view = LocalView.current
    return remember(view) { PetFeedbackController(view) }
}

fun recommendedDeviceRamMb(context: Context): Long {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val info = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(info)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        info.totalMem / (1024 * 1024)
    } else {
        4096L
    }
}
