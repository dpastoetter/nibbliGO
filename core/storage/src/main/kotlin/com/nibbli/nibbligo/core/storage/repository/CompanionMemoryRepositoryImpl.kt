package com.nibbli.nibbligo.core.storage.repository

import com.nibbli.nibbligo.core.domain.repository.CompanionMemoryRepository
import com.nibbli.nibbligo.core.model.CompanionMemoryFact
import com.nibbli.nibbligo.core.model.CompanionMemoryFactSource
import com.nibbli.nibbligo.core.model.CompanionMemoryLimits
import com.nibbli.nibbligo.core.storage.local.dao.CompanionMemoryFactDao
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import com.nibbli.nibbligo.core.storage.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionMemoryRepositoryImpl @Inject constructor(
    private val dao: CompanionMemoryFactDao,
) : CompanionMemoryRepository {

    override fun observeFacts(): Flow<List<CompanionMemoryFact>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getFacts(): List<CompanionMemoryFact> =
        dao.getAll().map { it.toDomain() }

    override suspend fun addFact(text: String, source: CompanionMemoryFactSource): CompanionMemoryFact {
        val trimmed = text.trim().take(CompanionMemoryLimits.MAX_FACT_CHARS)
        require(trimmed.isNotBlank())
        val existing = dao.getAll().map { it.toDomain() }
        existing.firstOrNull { it.text.equals(trimmed, ignoreCase = true) }?.let { return it }
        val newFact = CompanionMemoryFact(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            source = source,
            createdAtMillis = System.currentTimeMillis(),
        )
        val capped = trimToMaxFacts(existing + newFact)
        dao.deleteAll()
        capped.forEach { dao.insert(it.toEntity()) }
        return capped.last { it.text.equals(trimmed, ignoreCase = true) }
    }

    override suspend fun updateFact(id: String, text: String) {
        val trimmed = text.trim().take(CompanionMemoryLimits.MAX_FACT_CHARS)
        if (trimmed.isBlank()) {
            removeFact(id)
            return
        }
        val updated = dao.getAll().map { entity ->
            if (entity.id == id) entity.copy(text = trimmed) else entity
        }
        dao.deleteAll()
        updated.forEach { dao.insert(it) }
    }

    override suspend fun removeFact(id: String) {
        dao.delete(id)
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    override suspend fun replaceAll(facts: List<CompanionMemoryFact>) {
        dao.deleteAll()
        trimToMaxFacts(facts).forEach { dao.insert(it.toEntity()) }
    }

    private fun trimToMaxFacts(facts: List<CompanionMemoryFact>): List<CompanionMemoryFact> {
        val deduped = facts
            .map { it.copy(text = it.text.trim().take(CompanionMemoryLimits.MAX_FACT_CHARS)) }
            .filter { it.text.isNotBlank() }
            .distinctBy { it.text.lowercase() }
        val limited = deduped.takeLast(CompanionMemoryLimits.MAX_FACTS)
        var joined = joinFacts(limited.map { it.text })
        val mutable = limited.toMutableList()
        while (joined.length > CompanionMemoryLimits.MAX_RENDER_CHARS && mutable.size > 1) {
            mutable.removeAt(0)
            joined = joinFacts(mutable.map { it.text })
        }
        return mutable
    }

    private fun joinFacts(facts: List<String>): String =
        facts.take(CompanionMemoryLimits.MAX_FACTS)
            .joinToString(CompanionMemoryLimits.FACT_SEPARATOR)
            .take(CompanionMemoryLimits.MAX_RENDER_CHARS)
}
