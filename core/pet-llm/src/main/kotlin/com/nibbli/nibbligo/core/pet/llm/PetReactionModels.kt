package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState

data class PetReactionRequest(
    val state: PetState,
    val lastAction: String? = null,
    val userMessage: String? = null,
    val personality: PetPersonality = PetPersonality.PLAYFUL,
    /** Spontaneous ambient line while the user watches the pet home screen. */
    val moodPulse: Boolean = false,
    /** Recent lines the pet already said (for continuity). */
    val recentLines: List<String> = emptyList(),
    /** Context when reacting to app activity (agent, models, etc.). */
    val activityHint: String? = null,
)

data class PetReaction(
    val dialogue: String,
    val suggestedExpression: PetExpression? = null,
)

interface PetReactionPort {
    suspend fun generate(request: PetReactionRequest): PetReaction
}
