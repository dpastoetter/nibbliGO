package com.nibbli.nibbligo.feature.settings.presentation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthHandler
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.PetPersonality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val installedCount: Int = 0,
    val allowDownloads: Boolean = true,
    val storageSummary: String = "Calculating…",
    val hfSignedIn: Boolean = false,
    val hfConfigured: Boolean = false,
    val hfAuthMessage: String? = null,
    val hfManualTokenInput: String = "",
    val petPersonality: PetPersonality = PetPersonality.PLAYFUL,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
    private val huggingFaceAuthHandler: HuggingFaceAuthHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    modelRepository.observeInstalled(),
                    userPreferencesRepository.allowDownloads,
                    userPreferencesRepository.petPersonality,
                    userPreferencesRepository.themeMode,
                ) { installed, allowDownloads, personality, themeMode ->
                    SettingsPrefsSlice(installed, allowDownloads, personality, themeMode)
                },
                huggingFaceAuthRepository.accessToken,
            ) { prefs, token ->
                val bytes = prefs.installed.sumOf { it.sizeBytes }
                SettingsUiState(
                    installedCount = prefs.installed.size,
                    allowDownloads = prefs.allowDownloads,
                    storageSummary = "~${bytes / 1_000_000} MB in models (local)",
                    hfSignedIn = !token.isNullOrBlank(),
                    hfConfigured = huggingFaceAuthRepository.isConfigured(),
                    petPersonality = prefs.personality,
                    themeMode = prefs.themeMode,
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setAllowDownloads(allowed: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setAllowDownloads(allowed) }
    }

    fun clearChatHistory() {
        viewModelScope.launch { chatRepository.deleteAllConversations() }
    }

    fun signOutHuggingFace() {
        viewModelScope.launch { huggingFaceAuthRepository.clearToken() }
    }

    fun createHuggingFaceAuthIntent() = huggingFaceAuthRepository.createAuthIntent()

    fun onHuggingFaceAuthResult(intent: Intent?) {
        if (intent == null) return
        viewModelScope.launch {
            huggingFaceAuthHandler.handleAuthorizationResponse(intent)
                .onSuccess {
                    _uiState.update { it.copy(hfAuthMessage = "Signed in to Hugging Face") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(hfAuthMessage = e.message ?: "Hugging Face sign-in failed")
                    }
                }
        }
    }

    fun clearHfAuthMessage() {
        _uiState.update { it.copy(hfAuthMessage = null) }
    }

    fun onHfManualTokenChange(value: String) {
        _uiState.update { it.copy(hfManualTokenInput = value) }
    }

    fun setPetPersonality(personality: PetPersonality) {
        viewModelScope.launch { userPreferencesRepository.setPetPersonality(personality) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun saveManualHuggingFaceToken() {
        val token = _uiState.value.hfManualTokenInput.trim()
        if (token.isEmpty()) {
            _uiState.update { it.copy(hfAuthMessage = "Paste a token from huggingface.co/settings/tokens") }
            return
        }
        viewModelScope.launch {
            huggingFaceAuthRepository.saveAccessToken(token)
            _uiState.update {
                it.copy(
                    hfManualTokenInput = "",
                    hfAuthMessage = "Hugging Face token saved",
                )
            }
        }
    }
}

private data class SettingsPrefsSlice(
    val installed: List<InstalledModel>,
    val allowDownloads: Boolean,
    val personality: PetPersonality,
    val themeMode: AppThemeMode,
)
