package com.nibbli.nibbligo.core.agent.skills

import android.content.Context
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.InstalledSkillPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillPackageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolRegistry: ToolRegistry,
    private val skillPackageRepository: SkillPackageRepository,
) {
    private val skillsDir: File
        get() = File(context.filesDir, "skills").also { it.mkdirs() }

    suspend fun loadBundledSkills() {
        context.assets.list("skills")?.forEach { folder ->
            val skillId = folder
            val markdown = context.assets.open("skills/$folder/SKILL.md")
                .bufferedReader()
                .use { it.readText() }
            val hasJs = context.assets.list("skills/$folder")?.any { it.endsWith(".js") } == true
            installFromMarkdown(skillId, markdown, hasJs, isBundled = true)
        }
    }

    suspend fun importFromDirectory(sourceDir: File, skillId: String): Result<InstalledSkillPackage> {
        val skillMd = File(sourceDir, "SKILL.md")
        if (!skillMd.exists()) {
            return Result.failure(IllegalArgumentException("SKILL.md not found"))
        }
        val dest = File(skillsDir, skillId)
        dest.mkdirs()
        sourceDir.listFiles()?.forEach { file ->
            file.copyTo(File(dest, file.name), overwrite = true)
        }
        val markdown = skillMd.readText()
        val hasJs = dest.listFiles()?.any { it.extension == "js" } == true
        return installFromMarkdown(skillId, markdown, hasJs, isBundled = false)
    }

    private suspend fun installFromMarkdown(
        skillId: String,
        markdown: String,
        hasJs: Boolean,
        isBundled: Boolean,
    ): Result<InstalledSkillPackage> {
        val parsed = SkillManifestParser.parse(skillId, markdown, hasJs)
        toolRegistry.registerSkillTools(skillId, parsed.tools)
        val pkg = InstalledSkillPackage(
            skillId = skillId,
            displayName = parsed.displayName,
            description = parsed.description,
            localPath = File(skillsDir, skillId).absolutePath,
            version = parsed.version,
            permissions = parsed.permissions.joinToString(","),
            enabled = true,
            hasJsRuntime = hasJs,
            installedAtMillis = System.currentTimeMillis(),
        )
        skillPackageRepository.upsert(pkg)
        if (!isBundled) {
            val dest = File(skillsDir, skillId)
            dest.mkdirs()
            File(dest, "SKILL.md").writeText(markdown)
        }
        return Result.success(pkg)
    }

    suspend fun setEnabled(skillId: String, enabled: Boolean) {
        skillPackageRepository.setEnabled(skillId, enabled)
        if (!enabled) {
            toolRegistry.unregisterSkill(skillId)
        } else {
            val pkg = skillPackageRepository.get(skillId) ?: return
            val md = File(pkg.localPath, "SKILL.md")
            if (md.exists()) {
                val parsed = SkillManifestParser.parse(skillId, md.readText(), pkg.hasJsRuntime)
                toolRegistry.registerSkillTools(skillId, parsed.tools)
            }
        }
    }
}
