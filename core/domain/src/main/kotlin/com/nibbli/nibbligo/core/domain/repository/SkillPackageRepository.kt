package com.nibbli.nibbligo.core.domain.repository

import com.nibbli.nibbligo.core.model.InstalledSkillPackage
import kotlinx.coroutines.flow.Flow

interface SkillPackageRepository {
    fun observeAll(): Flow<List<InstalledSkillPackage>>
    suspend fun get(skillId: String): InstalledSkillPackage?
    suspend fun upsert(pkg: InstalledSkillPackage)
    suspend fun setEnabled(skillId: String, enabled: Boolean)
    suspend fun delete(skillId: String)
}

interface ActionHistoryRepository {
    suspend fun log(actionId: String, status: String, summary: String)
}
