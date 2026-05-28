package com.nibbli.nibbligo.core.model

/** Strict schemas for built-in skills — no free-form tool execution. */
sealed class SkillRequest {
    abstract val skillId: String

    data class NotesSkillRequest(
        val title: String,
        val body: String,
        val tags: List<String> = emptyList(),
    ) : SkillRequest() {
        override val skillId: String = SKILL_ID
        companion object {
            const val SKILL_ID = "notes"
        }
    }

    data class FileOrganizerSkillRequest(
        val directoryUri: String,
        val rule: String,
        val dryRun: Boolean = true,
    ) : SkillRequest() {
        override val skillId: String = SKILL_ID
        companion object {
            const val SKILL_ID = "file_organizer"
        }
    }

    data class RemindersSkillRequest(
        val title: String,
        val dueAtMillis: Long?,
        val notes: String,
    ) : SkillRequest() {
        override val skillId: String = SKILL_ID
        companion object {
            const val SKILL_ID = "reminders"
        }
    }
}

data class SkillDefinition(
    val id: String,
    val name: String,
    val description: String,
    val permissionRationale: String,
)
