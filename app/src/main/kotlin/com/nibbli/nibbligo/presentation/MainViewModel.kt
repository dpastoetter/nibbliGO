package com.nibbli.nibbligo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.assist.AssistNavigationBus
import com.nibbli.nibbligo.core.domain.repository.AccessibilityPreferencesRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    accessibilityPreferencesRepository: AccessibilityPreferencesRepository,
    assistNavigationBus: AssistNavigationBus,
) : ViewModel() {
    val themeMode: StateFlow<AppThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemeMode.SYSTEM)

    val accentPalette: StateFlow<AppAccentPalette> = userPreferencesRepository.accentPalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppAccentPalette.TEAL)

    val fontScale: StateFlow<Float> = accessibilityPreferencesRepository.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1.0f)

    val showOnboarding: StateFlow<Boolean> = userPreferencesRepository.onboardingCompleted
        .map { completed -> !completed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _agentNavigationSignal = MutableStateFlow(0)
    val agentNavigationSignal: StateFlow<Int> = _agentNavigationSignal.asStateFlow()

    init {
        viewModelScope.launch {
            assistNavigationBus.navigateToAgent.collect {
                _agentNavigationSignal.value += 1
            }
        }
    }
}
