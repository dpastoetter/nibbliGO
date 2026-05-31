package com.nibbli.nibbligo.core.domain.assist

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Opens Agent Chat from Home voice or other deep links. */
@Singleton
class AssistNavigationBus @Inject constructor(
    private val assistVoiceRequestBus: AssistVoiceRequestBus,
) {
    private val _navigateToAgent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToAgent: SharedFlow<Unit> = _navigateToAgent.asSharedFlow()

    fun openAgentWithMessage(transcript: String) {
        assistVoiceRequestBus.submitVoiceMessage(transcript)
        _navigateToAgent.tryEmit(Unit)
    }

    fun openAgentScreen() {
        _navigateToAgent.tryEmit(Unit)
    }
}
