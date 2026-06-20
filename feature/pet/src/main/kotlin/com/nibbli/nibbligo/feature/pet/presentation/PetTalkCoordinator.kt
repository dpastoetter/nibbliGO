package com.nibbli.nibbligo.feature.pet.presentation

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetState

/** Talk reaction merge extracted from [PetViewModel] for clarity and unit testing. */
internal object PetTalkCoordinator {
    fun mergeTalkReaction(
        postInteraction: PetState,
        dialogue: String,
        expression: PetExpression?,
    ): PetState = postInteraction.copy(
        dialogueLine = dialogue,
        expression = expression ?: postInteraction.expression,
    )
}
