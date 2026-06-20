package com.nibbli.nibbligo.feature.settings.presentation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.AccessibilityPreferencesRepository
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthHandler
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import com.nibbli.nibbligo.core.litert.engine.LiteRtEnginePool
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
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
    val installedModelIds: List<String> = emptyList(),
    val defaultModelId: String? = null,
    val allowDownloads: Boolean = true,
    val storageSummary: String = "Calculating…",
    val hfSignedIn: Boolean = false,
    val hfConfigured: Boolean = false,
    val hfAuthMessage: String? = null,
    val hfManualTokenInput: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentPalette: AppAccentPalette = AppAccentPalette.TEAL,
    val litertAccelerator: LiteRtAcceleratorPreference = LiteRtAcceleratorPreference.AUTO,
    val petNotificationsEnabled: Boolean = true,
    val fontScale: Float = 1.0f,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accessibilityPreferencesRepository: AccessibilityPreferencesRepository,
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
    private val huggingFaceAuthHandler: HuggingFaceAuthHandler,
    private val liteRtEnginePool: LiteRtEnginePool,
    private val liteRtModelPreloader: LiteRtModelPreloader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeInstalled(),
                userPreferencesRepository.defaultModelId,
                userPreferencesRepository.allowDownloads,
                userPreferencesRepository.themeMode,
                userPreferencesRepository.accentPalette,
                userPreferencesRepository.litertAccelerator,
                userPreferencesRepository.petNotificationsEnabled,
                huggingFaceAuthRepository.accessToken,
                accessibilityPreferencesRepository.fontScale,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val installed = values[0] as List<InstalledModel>
                val defaultModelId = values[1] as String?
                val allowDownloads = values[2] as Boolean
                val themeMode = values[3] as AppThemeMode
                val accentPalette = values[4] as AppAccentPalette
                val litertAccelerator = values[5] as LiteRtAcceleratorPreference
                val petNotifications = values[6] as Boolean
                val token = values[7] as String?
                val fontScale = values[8] as Float
                val bytes = installed.sumOf { it.sizeBytes }
                SettingsUiState(
                    installedCount = installed.size,
                    installedModelIds = installed.map { it.modelId },
                    defaultModelId = defaultModelId,
                    allowDownloads = allowDownloads,
                    storageSummary = "~${bytes / 1_000_000} MB in models (local)",
                    hfSignedIn = !token.isNullOrBlank(),
                    hfConfigured = huggingFaceAuthRepository.isConfigured(),
                    themeMode = themeMode,
                    accentPalette = accentPalette,
                    litertAccelerator = litertAccelerator,
                    petNotificationsEnabled = petNotifications,
                    fontScale = fontScale,
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(
                        hfAuthMessage = current.hfAuthMessage,
                        hfManualTokenInput = current.hfManualTokenInput,
                    )
                }
            }
        }
    }

    fun setAllowDownloads(allowed: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setAllowDownloads(allowed) }
    }

    fun setDefaultModelId(modelId: String?) {
        viewModelScope.launch { userPreferencesRepository.setDefaultModelId(modelId) }
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

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun setAccentPalette(palette: AppAccentPalette) {
        viewModelScope.launch { userPreferencesRepository.setAccentPalette(palette) }
    }

    fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) {
        viewModelScope.launch {
            userPreferencesRepository.setLitertAccelerator(preference)
            liteRtEnginePool.unloadAll()
            liteRtModelPreloader.reloadPrimaryModel()
        }
    }

    fun setPetNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPetNotificationsEnabled(enabled) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { accessibilityPreferencesRepository.setFontScale(scale) }
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
