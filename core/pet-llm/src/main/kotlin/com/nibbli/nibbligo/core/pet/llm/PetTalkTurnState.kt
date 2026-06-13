package com.nibbli.nibbligo.core.pet.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class LastTalkTurn(
    val userMessage: String,
    val petDialogue: String,
    val replySuggestions: List<String>,
    val timestampMillis: Long,
)

@Singleton
class PetTalkTurnState @Inject constructor() {
    private val _lastTurn = MutableStateFlow<LastTalkTurn?>(null)
    val lastTurn: StateFlow<LastTalkTurn?> = _lastTurn.asStateFlow()

    fun publish(
        userMessage: String,
        petDialogue: String,
        replySuggestions: List<String>,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        _lastTurn.value = LastTalkTurn(
            userMessage = userMessage.trim(),
            petDialogue = petDialogue.trim(),
            replySuggestions = replySuggestions,
            timestampMillis = timestampMillis,
        )
    }

    fun clear() {
        _lastTurn.value = null
    }
}
