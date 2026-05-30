package com.nibbli.nibbligo.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.agent.AgentOrchestrator
import com.nibbli.nibbligo.core.agent.PendingConfirmation
import com.nibbli.nibbligo.core.agent.tools.AgentToolPreview
import com.nibbli.nibbligo.core.domain.assist.AssistVoiceRequestBus
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.AgentSessionState
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.InstalledSkillPackage
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentChatUiState(
    val session: AgentSessionState = AgentSessionState(modelId = ""),
    val installedModelIds: List<String> = emptyList(),
    val skills: List<InstalledSkillPackage> = emptyList(),
    val input: String = "",
    val pendingConfirmation: PendingConfirmation? = null,
    val pendingToolTitle: String = "",
    val pendingToolPreview: String = "",
    val showThinkingTrace: Boolean = true,
    val error: String? = null,
    val supportsThinking: Boolean = false,
    val supportsToolCalling: Boolean = false,
    val agentModelMissing: Boolean = false,
)

@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val agentOrchestrator: AgentOrchestrator,
    private val modelRepository: ModelRepository,
    private val skillPackageRepository: SkillPackageRepository,
    private val actionHistoryRepository: ActionHistoryRepository,
    private val inferenceRuntime: InferenceRuntime,
    private val assistVoiceRequestBus: AssistVoiceRequestBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            assistVoiceRequestBus.voiceMessages.collect { transcript ->
                _uiState.update { it.copy(input = transcript, error = null) }
                send()
            }
        }
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            val modelId = resolveAgentModelId(installed)
            _uiState.update {
                it.copy(
                    installedModelIds = installed,
                    session = AgentSessionState(modelId = modelId),
                    supportsThinking = modelId.isNotEmpty() &&
                        inferenceRuntime.capabilitiesFor(modelId).supportsThinkingTrace,
                    supportsToolCalling = modelId.isNotEmpty() &&
                        inferenceRuntime.capabilitiesFor(modelId).supportsToolCalling,
                    agentModelMissing = FUNCTION_GEMMA_MODEL_ID !in installed,
                )
            }
            skillPackageRepository.observeAll().collect { skills ->
                _uiState.update { it.copy(skills = skills) }
            }
        }
    }

    fun updateInput(value: String) = _uiState.update { it.copy(input = value) }
    fun toggleThinking() = _uiState.update { it.copy(showThinkingTrace = !it.showThinkingTrace) }

    fun selectModel(modelId: String) {
        _uiState.update {
            it.copy(
                session = it.session.copy(modelId = modelId),
                supportsThinking = inferenceRuntime.capabilitiesFor(modelId).supportsThinkingTrace,
                supportsToolCalling = inferenceRuntime.capabilitiesFor(modelId).supportsToolCalling,
                error = null,
            )
        }
    }

    fun applyQuickPrompt(prompt: String) {
        _uiState.update { it.copy(input = prompt, error = null) }
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        val modelId = state.session.modelId
        if (text.isBlank() || modelId.isBlank()) return
        if (!inferenceRuntime.capabilitiesFor(modelId).supportsToolCalling) {
            _uiState.update {
                it.copy(
                    error = "Select FunctionGemma 270M for email and calendar actions. " +
                        "Install it under Manage → Models (Hugging Face sign-in required).",
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(input = "", session = it.session.copy(isRunning = true), error = null) }
            val result = agentOrchestrator.runTurn(modelId, text, state.session)
            handleRunResult(result)
        }
    }

    fun confirmTool() {
        val state = _uiState.value
        val pending = state.pendingConfirmation ?: return
        viewModelScope.launch {
            actionHistoryRepository.log(pending.call.toolId, "CONFIRMED", pending.call.argumentsJson)
            val result = agentOrchestrator.confirmAndContinue(
                state.session.modelId,
                state.session,
                pending,
            )
            handleRunResult(result)
        }
    }

    fun dismissTool() {
        _uiState.update {
            it.copy(
                pendingConfirmation = null,
                pendingToolTitle = "",
                pendingToolPreview = "",
                session = it.session.copy(isRunning = false),
            )
        }
    }

    fun modelLabel(modelId: String): String = ModelCatalog.displayName(modelId)

    private fun resolveAgentModelId(installed: List<String>): String {
        if (FUNCTION_GEMMA_MODEL_ID in installed) return FUNCTION_GEMMA_MODEL_ID
        return installed.firstOrNull { inferenceRuntime.capabilitiesFor(it).supportsToolCalling }.orEmpty()
    }

    private fun handleRunResult(result: com.nibbli.nibbligo.core.agent.AgentRunResult) {
        val pending = result.pendingConfirmation
        if (pending != null) {
            val call = pending.call
            _uiState.update {
                it.copy(
                    session = result.session,
                    pendingConfirmation = pending,
                    pendingToolTitle = AgentToolPreview.title(call.toolId),
                    pendingToolPreview = AgentToolPreview.description(call.toolId, call.argumentsJson),
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                session = result.session,
                pendingConfirmation = null,
                pendingToolTitle = "",
                pendingToolPreview = "",
                error = result.error,
            )
        }
    }

    private companion object {
        const val FUNCTION_GEMMA_MODEL_ID = "functiongemma-270m"
    }
}
