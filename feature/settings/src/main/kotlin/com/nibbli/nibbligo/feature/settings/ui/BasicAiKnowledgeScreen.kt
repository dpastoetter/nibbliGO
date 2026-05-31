package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nibbli.nibbligo.feature.settings.content.BasicAiKnowledgeContent

@Composable
fun BasicAiKnowledgeScreen(modifier: Modifier = Modifier) {
    FaqListScreen(
        title = "Basic AI Knowledge",
        subtitle = "What generative AI is, how it can fail, and how to use it responsibly.",
        sections = BasicAiKnowledgeContent.sections,
        modifier = modifier,
    )
}
