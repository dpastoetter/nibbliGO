package com.nibbli.nibbligo.core.pet.llm

/** Resolves which quick-reply chips to show above the talk bar. */
object PetTalkChipResolver {
    fun resolve(
        lastTurn: LastTalkTurn?,
        isGenerating: Boolean,
    ): List<String> {
        if (isGenerating) return emptyList()
        if (lastTurn == null) return PetTalkSuggestions.starterChips
        return lastTurn.replySuggestions.take(PetReplySuggestionSanitizer.MAX_SUGGESTIONS)
    }
}
