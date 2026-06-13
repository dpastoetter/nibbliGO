package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.repository.CompanionMemoryRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.CompanionMemoryFact
import com.nibbli.nibbligo.core.model.CompanionMemoryFactSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionMemoryStore @Inject constructor(
    private val companionMemoryRepository: CompanionMemoryRepository,
    private val petRepository: PetRepository,
) {
    private val migrationMutex = Mutex()
    private var migrated = false

    fun observeFacts(): Flow<List<CompanionMemoryFact>> = companionMemoryRepository.observeFacts()

    suspend fun ensureMigrated() {
        migrationMutex.withLock {
            if (migrated) return
            val pet = petRepository.getPetState()
            if (pet.memorySummary.isNotBlank() && companionMemoryRepository.getFacts().isEmpty()) {
                CompanionMemoryRenderer.parseFacts(pet.memorySummary).forEach { text ->
                    companionMemoryRepository.addFact(text, CompanionMemoryFactSource.MIGRATED)
                }
            }
            syncSummaryToPetState()
            migrated = true
        }
    }

    suspend fun syncSummaryToPetState() {
        val summary = renderSummary()
        val pet = petRepository.getPetState()
        if (pet.memorySummary != summary) {
            petRepository.savePetState(pet.copy(memorySummary = summary))
        }
    }

    suspend fun renderSummary(): String {
        val facts = companionMemoryRepository.getFacts()
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
        return CompanionMemoryRenderer.joinFacts(facts)
    }

    suspend fun addFact(text: String, source: CompanionMemoryFactSource): CompanionMemoryFact {
        ensureMigrated()
        val fact = companionMemoryRepository.addFact(text, source)
        syncSummaryToPetState()
        return fact
    }

    suspend fun updateFact(id: String, text: String) {
        ensureMigrated()
        companionMemoryRepository.updateFact(id, text)
        syncSummaryToPetState()
    }

    suspend fun removeFact(id: String) {
        ensureMigrated()
        companionMemoryRepository.removeFact(id)
        syncSummaryToPetState()
    }

    suspend fun clearAll() {
        ensureMigrated()
        companionMemoryRepository.clearAll()
        syncSummaryToPetState()
    }

    suspend fun carryOnHatch(maxFacts: Int = 3) {
        ensureMigrated()
        val carrySources = setOf(
            CompanionMemoryFactSource.USER_APPROVED,
            CompanionMemoryFactSource.MANUAL,
        )
        val toCarry = companionMemoryRepository.getFacts()
            .filter { it.source in carrySources }
            .takeLast(maxFacts)
        companionMemoryRepository.clearAll()
        toCarry.forEach { fact ->
            companionMemoryRepository.addFact(fact.text, CompanionMemoryFactSource.EVOLUTION)
        }
        if (toCarry.isEmpty()) {
            syncSummaryToPetState()
            return
        }
        syncSummaryToPetState()
    }
}
