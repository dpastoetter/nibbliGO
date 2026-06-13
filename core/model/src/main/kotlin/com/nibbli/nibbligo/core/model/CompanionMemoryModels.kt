package com.nibbli.nibbligo.core.model

enum class CompanionMemoryFactSource {
    USER_APPROVED,
    MANUAL,
    ONBOARDING,
    EVOLUTION,
    MIGRATED,
}

object CompanionMemoryLimits {
    const val MAX_FACTS = 12
    const val MAX_RENDER_CHARS = 600
    const val MAX_FACT_CHARS = 80
    const val FACT_SEPARATOR = " • "
}

data class CompanionMemoryFact(
    val id: String,
    val text: String,
    val source: CompanionMemoryFactSource,
    val createdAtMillis: Long,
)
