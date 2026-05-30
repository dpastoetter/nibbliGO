package com.nibbli.nibbligo.core.pet.llm

/** Shared limits for user-initiated Home talk (not mood pulse / care reactions). */
object PetTalkLimits {
    /** Safety ceiling for garbled loops or prompt leaks — not a target reply length. */
    const val RUNAWAY_MAX_CHARS = 2048
}
