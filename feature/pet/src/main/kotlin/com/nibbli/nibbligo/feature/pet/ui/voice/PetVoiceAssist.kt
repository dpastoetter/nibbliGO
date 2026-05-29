package com.nibbli.nibbligo.feature.pet.ui.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** FUTO Voice Input — supports [RecognizerIntent.ACTION_RECOGNIZE_SPEECH], not [SpeechRecognizer]. */
private const val FUTO_VOICE_INPUT_PACKAGE = "org.futo.voiceinput"

@Composable
fun rememberVoiceAssistLauncher(
    onListening: () -> Unit,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onStopped: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    fun handleTranscript(text: String) {
        onStopped()
        if (text.isNotEmpty()) {
            onResult(text)
        } else {
            onError("Didn't catch that — try again")
        }
    }

    val recognizeSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        destroyRecognizer()
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                handleTranscript(matches?.firstOrNull()?.trim().orEmpty())
            }
            Activity.RESULT_CANCELED -> onStopped()
            else -> {
                onStopped()
                onError("Voice input failed")
            }
        }
    }

    fun startSpeechRecognizerFallback() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("No speech recognition app found. Install FUTO Voice Input or Google app.")
            onStopped()
            return
        }
        destroyRecognizer()
        onListening()
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer
        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    destroyRecognizer()
                    handleTranscript(matches?.firstOrNull()?.trim().orEmpty())
                }

                override fun onError(error: Int) {
                    destroyRecognizer()
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy — try again"
                            SpeechRecognizer.ERROR_SERVER -> "Recognizer server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Voice input failed"
                        }
                        onError(message)
                    }
                    onStopped()
                }
            },
        )
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to nibbli Assist…")
        }
        speechRecognizer.startListening(intent)
    }

    fun startRecognition() {
        val launchIntent = buildRecognizeSpeechLaunchIntent(context)
        if (launchIntent != null) {
            onListening()
            recognizeSpeechLauncher.launch(launchIntent)
        } else {
            startSpeechRecognizerFallback()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecognition()
        } else {
            onError("Microphone permission is required for voice assist")
            onStopped()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            destroyRecognizer()
        }
    }

    return {
        when (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        ) {
            PackageManager.PERMISSION_GRANTED -> startRecognition()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/**
 * Builds an intent for the standard Android speech UI (FUTO Voice Input, Google, etc.).
 * Returns a package-targeted intent, a chooser, or null if nothing can handle recognition.
 */
internal fun buildRecognizeSpeechLaunchIntent(context: Context): Intent? {
    val base = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to nibbli Assist…")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }
    val handlers = queryRecognizeSpeechHandlers(context, base)
    if (handlers.isEmpty()) return null

    val futo = handlers.firstOrNull { it.activityInfo.packageName == FUTO_VOICE_INPUT_PACKAGE }
    if (futo != null) {
        return Intent(base).setPackage(FUTO_VOICE_INPUT_PACKAGE)
    }
    if (handlers.size == 1) {
        return Intent(base).setPackage(handlers.first().activityInfo.packageName)
    }
    return Intent.createChooser(base, "Choose voice input")
}

private fun queryRecognizeSpeechHandlers(
    context: Context,
    intent: Intent,
): List<android.content.pm.ResolveInfo> {
    val pm = context.packageManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
}
