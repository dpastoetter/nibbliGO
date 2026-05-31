package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nibbli.nibbligo.feature.settings.content.NibbliGoFaqContent

@Composable
fun NibbliFaqScreen(modifier: Modifier = Modifier) {
    FaqListScreen(
        title = "FAQ for nibbliGO",
        subtitle = "How the app works — models, privacy, Home, and your Pixel Friend.",
        sections = NibbliGoFaqContent.sections,
        modifier = modifier,
    )
}
