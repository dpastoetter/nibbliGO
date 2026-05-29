package com.nibbli.nibbligo.core.domain.assist

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delivers voice transcripts from Home to Agent Chat for an on-device assist turn.
 */
@Singleton
class AssistVoiceRequestBus @Inject constructor() {
    private val _voiceMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val voiceMessages: SharedFlow<String> = _voiceMessages.asSharedFlow()

    fun submitVoiceMessage(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isNotEmpty()) {
            _voiceMessages.tryEmit(trimmed)
        }
    }
}
