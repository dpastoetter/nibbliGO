package com.nibbli.nibbligo.core.pet.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetTalkTurnCoordinator @Inject constructor(
    private val petReactionPort: PetReactionPort,
    private val petTalkTurnState: PetTalkTurnState,
) {
    suspend fun resolveUserTalkReaction(
        raw: String,
        request: PetReactionRequest,
    ): PetReaction {
        val parsed = PetReplySuggestionParser.parseTalkWithSuggestions(
            raw = raw,
            petName = request.state.name,
            caretakerName = request.caretakerName,
            lastUserMessage = request.userMessage,
        )
        var reaction = parsed.reaction
        val userMessage = request.userMessage?.trim().orEmpty()
        if (userMessage.isNotEmpty() && reaction.replySuggestions.isEmpty()) {
            reaction = reaction.copy(
                replySuggestions = petReactionPort.generateReplySuggestions(
                    userMessage = userMessage,
                    petDialogue = reaction.dialogue,
                    request = request,
                ),
            )
        }
        return reaction
    }

    fun publishTurn(
        userMessage: String,
        petDialogue: String,
        replySuggestions: List<String>,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val user = userMessage.trim()
        val dialogue = petDialogue.trim()
        if (user.isEmpty() || dialogue.isEmpty()) return
        petTalkTurnState.publish(
            userMessage = user,
            petDialogue = dialogue,
            replySuggestions = replySuggestions,
            timestampMillis = timestampMillis,
        )
    }

    fun clearTurn() {
        petTalkTurnState.clear()
    }
}
