package com.nibbli.nibbligo.core.agent.tools

import com.nibbli.nibbligo.core.model.AgentTool
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import com.nibbli.nibbligo.core.model.ToolSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor() {

    private val _tools = MutableStateFlow(BuiltinTools.all.toList())
    val tools: StateFlow<List<AgentTool>> = _tools.asStateFlow()

    fun allTools(): List<AgentTool> = _tools.value

    fun findTool(id: String): AgentTool? = _tools.value.find { it.id == id || it.name == id }

    fun registerSkillTools(skillId: String, tools: List<AgentTool>) {
        val withSource = tools.map {
            it.copy(source = ToolSource.SKILL_PACKAGE, skillId = skillId)
        }
        _tools.update { current ->
            val filtered = current.filter { it.skillId != skillId }
            filtered + withSource
        }
    }

    fun unregisterSkill(skillId: String) {
        _tools.update { it.filter { it.skillId != skillId } }
    }

    fun registerActionTools(actions: List<AgentTool>) {
        val withSource = actions.map { it.copy(source = ToolSource.ACTION) }
        _tools.update { current ->
            val withoutActions = current.filter { it.source != ToolSource.ACTION }
            withoutActions + withSource
        }
    }

    fun requiresConfirmation(tool: AgentTool): Boolean =
        tool.riskLevel == ToolRiskLevel.SENSITIVE
}
