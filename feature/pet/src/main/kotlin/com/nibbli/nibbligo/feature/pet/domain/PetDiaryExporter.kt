package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PetDiaryExporter {
    fun exportMarkdown(state: PetState): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return buildString {
            appendLine("# nibbli diary")
            appendLine()
            appendLine("Exported: $date")
            appendLine()
            appendLine("## ${state.name}")
            appendLine("- Stage: ${state.stage.name}")
            appendLine("- Age: ${state.ageMinutes} minutes")
            appendLine("- Care score: ${state.careScore}/100")
            appendLine()
            appendLine("## Stats")
            appendLine("- Hunger: ${state.stats.hunger}")
            appendLine("- Happiness: ${state.stats.happiness}")
            appendLine("- Energy: ${state.stats.energy}")
            appendLine("- Hygiene: ${state.stats.hygiene}")
            appendLine("- Health: ${state.stats.health}")
            appendLine()
            if (state.memorySummary.isNotBlank()) {
                appendLine("## Memory")
                appendLine(state.memorySummary)
                appendLine()
            }
            appendLine("## Last words")
            appendLine("> ${state.dialogueLine}")
        }
    }
}
