package com.nibbli.nibbligo.feature.actions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.agent.skills.SkillPackageLoader
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.mcp.McpServerConfig
import com.nibbli.nibbligo.core.mcp.McpServerStore
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.ActionDraft
import com.nibbli.nibbligo.core.model.InstalledSkillPackage
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.SafeAction
import com.nibbli.nibbligo.core.model.SkillDefinition
import com.nibbli.nibbligo.core.model.SkillRequest
import com.nibbli.nibbligo.feature.actions.domain.ActionExecutor
import com.nibbli.nibbligo.feature.actions.domain.ActionRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActionsUiState(
    val actions: List<SafeAction> = ActionRegistry.actions,
    val builtinSkills: List<SkillDefinition> = listOf(
        SkillDefinition(
            SkillRequest.NotesSkillRequest.SKILL_ID,
            "Notes",
            "Save structured notes locally",
            "Writes to your local notes store only.",
        ),
        SkillDefinition(
            SkillRequest.FileOrganizerSkillRequest.SKILL_ID,
            "File organizer",
            "Preview renames with a rule",
            "Dry-run by default; no silent file changes.",
        ),
        SkillDefinition(
            SkillRequest.RemindersSkillRequest.SKILL_ID,
            "Reminders",
            "Create task/reminder entries",
            "Stored on-device until you export.",
        ),
    ),
    val installedSkillPackages: List<InstalledSkillPackage> = emptyList(),
    val petTaskMode: Boolean = false,
    val pendingDraft: ActionDraft? = null,
    val pendingSkillSummary: String? = null,
    val resultMessage: String? = null,
    val mcpServers: List<McpServerConfig> = emptyList(),
    val mcpServerUrlInput: String = "",
)

@HiltViewModel
class ActionsViewModel @Inject constructor(
    private val petEventBus: PetEventBus,
    private val skillPackageRepository: SkillPackageRepository,
    private val actionHistoryRepository: ActionHistoryRepository,
    private val toolRegistry: ToolRegistry,
    private val skillPackageLoader: SkillPackageLoader,
    private val mcpServerStore: McpServerStore,
    private val mcpToolRegistry: McpToolRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionsUiState())
    val uiState: StateFlow<ActionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            skillPackageRepository.observeAll().collect { packages ->
                _uiState.update { it.copy(installedSkillPackages = packages) }
            }
        }
        viewModelScope.launch {
            mcpServerStore.servers.collect { servers ->
                _uiState.update { it.copy(mcpServers = servers) }
            }
        }
    }

    fun updateMcpUrlInput(value: String) = _uiState.update { it.copy(mcpServerUrlInput = value) }

    fun addMcpServer() {
        val url = _uiState.value.mcpServerUrlInput.trim()
        if (url.isBlank()) return
        val id = url.hashCode().toString()
        viewModelScope.launch {
            val server = McpServerConfig(id = id, name = "MCP $id", url = url)
            mcpServerStore.add(server)
            mcpToolRegistry.refresh(server).onSuccess { count ->
                toolRegistry.registerMcpTools(mcpToolRegistry.allTools())
                _uiState.update {
                    it.copy(mcpServerUrlInput = "", resultMessage = "MCP: discovered $count tools")
                }
            }
        }
    }

    fun togglePetTaskMode() = _uiState.update { it.copy(petTaskMode = !it.petTaskMode) }

    fun requestAction(actionId: String) {
        val draft = ActionExecutor.buildDraft(actionId, _uiState.value.petTaskMode) ?: return
        _uiState.update { it.copy(pendingDraft = draft, pendingSkillSummary = null) }
    }

    fun requestSkill(skillId: String) {
        val summary = when (skillId) {
            SkillRequest.NotesSkillRequest.SKILL_ID ->
                "Save note \"My thought\" with tags [ideas]?"
            SkillRequest.FileOrganizerSkillRequest.SKILL_ID ->
                "Preview rename in ~/Downloads with rule vacation_* ?"
            SkillRequest.RemindersSkillRequest.SKILL_ID ->
                "Create reminder \"Try benchmark\" due tomorrow?"
            else -> {
                val pkg = _uiState.value.installedSkillPackages.find { it.skillId == skillId }
                "Run skill \"${pkg?.displayName ?: skillId}\" on-device?"
            }
        }
        _uiState.update { it.copy(pendingSkillSummary = summary, pendingDraft = null) }
    }

    fun toggleSkillEnabled(skillId: String, enabled: Boolean) {
        viewModelScope.launch {
            skillPackageLoader.setEnabled(skillId, enabled)
        }
    }

    fun confirmPending() {
        val draft = _uiState.value.pendingDraft
        val skill = _uiState.value.pendingSkillSummary
        viewModelScope.launch {
            val message = when {
                draft != null -> {
                    actionHistoryRepository.log(draft.actionId, "COMPLETED", draft.title)
                    ActionExecutor.executeConfirmed(draft.actionId, _uiState.value.petTaskMode)
                }
                skill != null -> {
                    actionHistoryRepository.log("skill", "COMPLETED", skill)
                    "Skill approved: $skill (stored locally)"
                }
                else -> return@launch
            }
            petEventBus.emit(PetEvent.ActionCompleted)
            _uiState.update {
                it.copy(pendingDraft = null, pendingSkillSummary = null, resultMessage = message)
            }
        }
    }

    fun dismissPending() = _uiState.update { it.copy(pendingDraft = null, pendingSkillSummary = null) }

    fun toolCount(): Int = toolRegistry.allTools().size
}
