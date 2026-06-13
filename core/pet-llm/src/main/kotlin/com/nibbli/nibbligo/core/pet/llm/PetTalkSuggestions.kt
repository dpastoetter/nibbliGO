package com.nibbli.nibbligo.core.pet.llm

/** Static starter chips shown before the first home/chat talk turn. */
object PetTalkSuggestions {

    const val LETS_PLAY_CHIP = "Let's play!"

    val starterChips: List<String> = listOf(
        "How are you?",
        "Good morning",
        LETS_PLAY_CHIP,
    )
}
