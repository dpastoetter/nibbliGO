package com.nibbli.nibbligo.core.pet.llm

data class PetGameFaqEntry(
    val id: String,
    val topics: Set<String>,
    val keywords: Set<String>,
    val answer: String,
)
