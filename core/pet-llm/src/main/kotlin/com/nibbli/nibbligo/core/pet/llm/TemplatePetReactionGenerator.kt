package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplatePetReactionGenerator @Inject constructor() : PetReactionPort {
    override suspend fun generate(request: PetReactionRequest): PetReaction {
        val s = request.state
        val dialogue = when {
            request.userMessage != null -> talkReply(request.userMessage)
            request.lastAction != null -> actionReply(request.lastAction)
            s.activeNeed != PetNeed.NONE -> needReply(s.activeNeed)
            else -> "I'm here on your device, ${s.name}!"
        }
        return PetReaction(
            dialogue = dialogue.take(160),
            suggestedExpression = expressionFor(s.activeNeed),
        )
    }

    private fun talkReply(msg: String): String = when {
        msg.contains("how are", ignoreCase = true) -> "I'm doing okay! Stats change as we hang out."
        msg.contains("night", ignoreCase = true) -> "Sleepy time? I'll rest on-device."
        msg.contains("back", ignoreCase = true) -> "Welcome back! I missed you."
        else -> "I hear you! Everything stays local."
    }

    private fun actionReply(action: String): String = "Thanks for $action — that helped!"

    private fun needReply(need: PetNeed): String = when (need) {
        PetNeed.HUNGRY -> "Beep… food please?"
        PetNeed.SICK -> "I feel icky… medicine?"
        PetNeed.DIRTY -> "It's messy here!"
        PetNeed.TIRED -> "Yawn… so tired."
        PetNeed.UNHAPPY -> "Cheer me up?"
        PetNeed.LONELY -> "Visit me soon?"
        else -> "Beep beep!"
    }

    private fun expressionFor(need: PetNeed): PetExpression? = when (need) {
        PetNeed.HUNGRY -> PetExpression.HUNGRY
        PetNeed.SICK -> PetExpression.SICK
        PetNeed.TIRED -> PetExpression.SLEEPY
        PetNeed.NONE -> PetExpression.HAPPY
        else -> PetExpression.ATTENTION
    }
}
