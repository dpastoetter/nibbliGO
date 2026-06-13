package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.core.storage.work.ModelDownloadWorker
import com.nibbli.nibbligo.feature.pet.ui.feedback.recommendedDeviceRamMb
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PetOnboardingUiState(
    val stepIndex: Int = 0,
    val caretakerName: String = "",
    val petName: String = "nibbli",
    val personality: PetPersonality = PetPersonality.PLAYFUL,
    val aboutYou: String = "",
    val companionGoal: String = "",
    val termsAccepted: Boolean = false,
    val isSaving: Boolean = false,
    val recommendedModelId: String = ModelCatalog.RECOMMENDED_PET_MODEL_ID,
    val isDownloadingModel: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadResuming: Boolean = false,
    val downloadMessage: String? = null,
) {
    val stepCount: Int = 7

    val canContinue: Boolean
        get() = when (stepIndex) {
            0 -> true
            1 -> petName.trim().isNotBlank()
            2 -> caretakerName.trim().isNotBlank()
            3, 4 -> true
            5 -> termsAccepted
            6 -> true
            else -> false
        }
}

@HiltViewModel
class PetOnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val petRepository: PetRepository,
    private val modelRepository: ModelRepository,
    private val liteRtModelPreloader: LiteRtModelPreloader,
    private val inferenceRuntime: InferenceRuntime,
    private val petModelResolver: PetModelResolver,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) : ViewModel() {

    private val workManager by lazy {
        runCatching { WorkManager.getInstance(context) }.getOrNull()
    }
    private val _uiState = MutableStateFlow(PetOnboardingUiState())
    val uiState: StateFlow<PetOnboardingUiState> = _uiState.asStateFlow()

    init {
        val recommended = ModelCatalog.recommendedPetModelId(recommendedDeviceRamMb(context))
        _uiState.update { it.copy(recommendedModelId = recommended) }
        viewModelScope.launch {
            val pet = petRepository.getPetState()
            val profile = userPreferencesRepository.petOnboardingProfile.first()
            val personality = userPreferencesRepository.petPersonality.first()
            _uiState.update {
                it.copy(
                    petName = pet.name.ifBlank { "nibbli" },
                    caretakerName = profile.caretakerName,
                    aboutYou = profile.aboutYou,
                    companionGoal = profile.companionGoal,
                    personality = personality,
                )
            }
        }
        observeDownloadProgress()
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            val manager = workManager ?: return@launch
            val modelId = _uiState.value.recommendedModelId
            runCatching {
                manager.getWorkInfosByTagFlow(ModelDownloadWorker.WORK_TAG)
            }.getOrElse { kotlinx.coroutines.flow.flowOf(emptyList()) }
                .collect { workInfos ->
                    val active = workInfos.filter {
                        it.state in setOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED) &&
                            it.tags.any { tag -> tag == "hf_download_$modelId" }
                    }
                    val activeWork = active.firstOrNull()
                    val progress = activeWork
                        ?.progress
                        ?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0) ?: 0
                    val resuming = activeWork
                        ?.progress
                        ?.getBoolean(ModelDownloadWorker.KEY_IS_RESUMING, false) ?: false
                    _uiState.update {
                        it.copy(
                            isDownloadingModel = active.isNotEmpty(),
                            downloadProgress = progress,
                            downloadResuming = resuming,
                        )
                    }
                }
        }
    }

    fun updateCaretakerName(value: String) {
        _uiState.update { it.copy(caretakerName = value) }
    }

    fun updatePetName(value: String) {
        _uiState.update { it.copy(petName = value) }
    }

    fun updatePersonality(personality: PetPersonality) {
        _uiState.update { it.copy(personality = personality) }
    }

    fun updateAboutYou(value: String) {
        _uiState.update { it.copy(aboutYou = value) }
    }

    fun updateCompanionGoal(value: String) {
        _uiState.update { it.copy(companionGoal = value) }
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.stepIndex >= state.stepCount - 1) state
            else state.copy(stepIndex = state.stepIndex + 1)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.stepIndex <= 0) state
            else state.copy(stepIndex = state.stepIndex - 1)
        }
    }

    fun downloadRecommendedModel() {
        viewModelScope.launch {
            val modelId = _uiState.value.recommendedModelId
            modelRepository.install(modelId)
                .onSuccess {
                    _uiState.update { it.copy(downloadMessage = "Downloading on-device model…") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(downloadMessage = e.message ?: "Download failed")
                    }
                }
        }
    }

    fun clearDownloadMessage() {
        _uiState.update { it.copy(downloadMessage = null) }
    }

    fun complete(onFinished: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        if (!state.termsAccepted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val trimmedPetName = state.petName.trim().ifBlank { "nibbli" }
                val pet = petRepository.getPetState()
                if (pet.name != trimmedPetName) {
                    petRepository.savePetState(pet.copy(name = trimmedPetName))
                }
                val profile = PetOnboardingProfile(
                    caretakerName = state.caretakerName.trim(),
                    aboutYou = state.aboutYou.trim(),
                    companionGoal = state.companionGoal.trim(),
                    completed = true,
                )
                userPreferencesRepository.setPetOnboardingProfile(profile)
                userPreferencesRepository.setTermsAccepted(System.currentTimeMillis())
                userPreferencesRepository.setPetPersonality(state.personality)
                liteRtModelPreloader.invalidate()
                runCatching {
                    if (modelAvailabilityGate.hasUsableModel()) {
                        inferenceRuntime.resetHomeTalkSession(petModelResolver.resolve())
                    }
                }
                liteRtModelPreloader.preloadPrimaryModel(force = true)
                onFinished()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
