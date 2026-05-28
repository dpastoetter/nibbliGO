package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val installedCount: Int = 0,
    val allowDownloads: Boolean = true,
    val useLiteRtRuntime: Boolean = false,
    val liteRtModelPresent: Boolean = false,
    val storageSummary: String = "Calculating…",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeInstalled(),
                userPreferencesRepository.allowDownloads,
                userPreferencesRepository.preferredRuntimeKind,
            ) { installed, allowDownloads, runtimeKind ->
                val bytes = installed.sumOf { it.sizeBytes }
                SettingsUiState(
                    installedCount = installed.size,
                    allowDownloads = allowDownloads,
                    useLiteRtRuntime = runtimeKind == "litert",
                    storageSummary = "~${bytes / 1_000_000} MB in models (local)",
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setAllowDownloads(allowed: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setAllowDownloads(allowed) }
    }

    fun setUseLiteRt(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredRuntimeKind(if (enabled) "litert" else "fake")
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch { chatRepository.deleteAllConversations() }
    }
}
