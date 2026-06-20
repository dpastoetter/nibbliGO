package com.nibbli.nibbligo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.feature.pet.ui.PetOnboardingScreen
import com.nibbli.nibbligo.navigation.NibbliApp
import com.nibbli.nibbligo.presentation.MainViewModel

@Composable
fun NibbliAppWithTheme(viewModel: MainViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentPalette by viewModel.accentPalette.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) onboardingDismissed = true
    }

    NibbliTheme(themeMode = themeMode, accentPalette = accentPalette) {
        val baseDensity = LocalDensity.current
        val scaledDensity = Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale,
        )
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            if (!onboardingDismissed) {
                PetOnboardingScreen(onFinished = { onboardingDismissed = true })
            } else {
                NibbliApp()
            }
        }
    }
}
