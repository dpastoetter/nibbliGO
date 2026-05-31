package com.nibbli.nibbligo.feature.settings.content

data class FaqItem(
    val question: String,
    val answer: String,
)

data class FaqSection(
    val title: String,
    val items: List<FaqItem>,
)
