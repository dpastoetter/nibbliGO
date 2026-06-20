package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetDiaryExporterTest {

    @Test
    fun exportMarkdown_includesCoreSections() {
        val markdown = PetDiaryExporter.exportMarkdown(
            PetState(
                name = "Pixel",
                stage = LifeStage.CHILD,
                ageMinutes = 120,
                careScore = 55,
                stats = PetStats(hunger = 70, mood = 80, energy = 60, hygiene = 50, health = 90),
                memorySummary = "Loves dinosaurs.",
                dialogueLine = "Let's play!",
            ),
        )
        assertTrue(markdown.contains("# nibbli diary"))
        assertTrue(markdown.contains("## Pixel"))
        assertTrue(markdown.contains("Stage: CHILD"))
        assertTrue(markdown.contains("## Stats"))
        assertTrue(markdown.contains("## Memory"))
        assertTrue(markdown.contains("Loves dinosaurs."))
        assertTrue(markdown.contains("## Last words"))
        assertTrue(markdown.contains("> Let's play!"))
    }

    @Test
    fun exportMarkdown_omitsBlankMemory() {
        val markdown = PetDiaryExporter.exportMarkdown(PetState(memorySummary = "  "))
        assertFalse(markdown.contains("## Memory"))
    }

    @Test
    fun exportMarkdown_preservesSpecialCharacters() {
        val markdown = PetDiaryExporter.exportMarkdown(
            PetState(
                name = "Pi*x",
                dialogueLine = "I said \"hello\" & waved",
            ),
        )
        assertTrue(markdown.contains("Pi*x"))
        assertTrue(markdown.contains("I said \"hello\" & waved"))
    }
}
