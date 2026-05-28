package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthHandler
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import android.content.Intent
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
    val useLiteRtRuntime: Boolean = false,
    val liteRtModelPresent: Boolean = false,
    val storageSummary: String = "Calculating…",
    val hfSignedIn: Boolean = false,
    val hfConfigured: Boolean = false,
    val hfAuthMessage: String? = null,
    val hfManualTokenInput: String = "",
    val usePetLlmReactions: Boolean = true,
    val petPersonality: PetPersonality = PetPersonality.PLAYFUL,
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
                    userPreferencesRepository.preferredRuntimeKind,
                    userPreferencesRepository.usePetLlmReactions,
                    userPreferencesRepository.petPersonality,
                ) { installed, allowDownloads, runtimeKind, usePetLlm, personality ->
                    Quintuple(installed, allowDownloads, runtimeKind, usePetLlm, personality)
                },
                huggingFaceAuthRepository.accessToken,
            ) { prefs, token ->
                val bytes = prefs.installed.sumOf { it.sizeBytes }
                SettingsUiState(
                    installedCount = prefs.installed.size,
                    allowDownloads = prefs.allowDownloads,
                    useLiteRtRuntime = prefs.runtimeKind == "litert",
                    storageSummary = "~${bytes / 1_000_000} MB in models (local)",
                    hfSignedIn = !token.isNullOrBlank(),
                    hfConfigured = huggingFaceAuthRepository.isConfigured(),
                    usePetLlmReactions = prefs.usePetLlm,
                    petPersonality = prefs.personality,
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

    fun setUsePetLlmReactions(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUsePetLlmReactions(enabled) }
    }

    fun setPetPersonality(personality: PetPersonality) {
        viewModelScope.launch { userPreferencesRepository.setPetPersonality(personality) }
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

private data class Quintuple(
    val installed: List<InstalledModel>,
    val allowDownloads: Boolean,
    val runtimeKind: String,
    val usePetLlm: Boolean,
    val personality: PetPersonality,
)
