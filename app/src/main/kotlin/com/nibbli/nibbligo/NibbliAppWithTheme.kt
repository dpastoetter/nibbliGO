package com.nibbli.nibbligo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.feature.pet.ui.PetOnboardingScreen
import com.nibbli.nibbligo.navigation.NibbliApp
import com.nibbli.nibbligo.presentation.MainViewModel

@Composable
fun NibbliAppWithTheme(viewModel: MainViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }

    NibbliTheme(themeMode = themeMode) {
        if (showOnboarding && !onboardingDismissed) {
            PetOnboardingScreen(onFinished = { onboardingDismissed = true })
        } else {
            NibbliApp()
        }
    }
}
