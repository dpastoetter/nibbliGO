package com.nibbli.nibbligo.core.agent.skills

import com.nibbli.nibbligo.core.model.AgentTool
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import com.nibbli.nibbligo.core.model.ToolSource

data class ParsedSkillManifest(
    val skillId: String,
    val displayName: String,
    val description: String,
    val version: String,
    val permissions: List<String>,
    val tools: List<AgentTool>,
    val hasJsRuntime: Boolean,
)

object SkillManifestParser {

    fun parse(skillId: String, markdown: String, hasJsRuntime: Boolean): ParsedSkillManifest {
        val frontmatter = extractFrontmatter(markdown)
        val displayName = frontmatter["name"] ?: skillId
        val description = frontmatter["description"] ?: "Imported skill"
        val version = frontmatter["version"] ?: "1.0.0"
        val permissions = frontmatter["permissions"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()
        val tools = parseToolsFromBody(skillId, markdown)
        return ParsedSkillManifest(
            skillId = skillId,
            displayName = displayName,
            description = description,
            version = version,
            permissions = permissions,
            tools = tools,
            hasJsRuntime = hasJsRuntime,
        )
    }

    private fun extractFrontmatter(markdown: String): Map<String, String> {
        if (!markdown.startsWith("---")) return emptyMap()
        val end = markdown.indexOf("---", 3)
        if (end < 0) return emptyMap()
        val block = markdown.substring(3, end).trim()
        return block.lines()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()
    }

    private fun parseToolsFromBody(skillId: String, markdown: String): List<AgentTool> {
        val tools = mutableListOf<AgentTool>()
        val toolBlockRegex = Regex("""### tool:\s*(\w+)""", RegexOption.IGNORE_CASE)
        toolBlockRegex.findAll(markdown).forEach { match ->
            val toolName = match.groupValues[1]
            tools.add(
                AgentTool(
                    id = "${skillId}_$toolName",
                    name = toolName,
                    description = "Tool from skill $skillId",
                    parametersJsonSchema = """{"type":"object","properties":{"input":{"type":"string"}}}""",
                    riskLevel = ToolRiskLevel.SENSITIVE,
                    source = ToolSource.SKILL_PACKAGE,
                    skillId = skillId,
                ),
            )
        }
        if (tools.isEmpty()) {
            tools.add(
                AgentTool(
                    id = "${skillId}_run",
                    name = "run",
                    description = descriptionFromMarkdown(markdown),
                    parametersJsonSchema = """{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}""",
                    riskLevel = ToolRiskLevel.SENSITIVE,
                    source = ToolSource.SKILL_PACKAGE,
                    skillId = skillId,
                ),
            )
        }
        return tools
    }

    private fun descriptionFromMarkdown(markdown: String): String {
        val line = markdown.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }
        return line?.take(120) ?: "Skill tool"
    }
}
