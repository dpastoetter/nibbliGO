package com.nibbli.nibbligo.core.storage.repository

import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.InstalledSkillPackage
import com.nibbli.nibbligo.core.storage.local.dao.ActionHistoryDao
import com.nibbli.nibbligo.core.storage.local.dao.SkillInstallDao
import com.nibbli.nibbligo.core.storage.local.entity.ActionHistoryEntity
import com.nibbli.nibbligo.core.storage.local.entity.SkillInstallEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillPackageRepositoryImpl @Inject constructor(
    private val skillInstallDao: SkillInstallDao,
) : SkillPackageRepository {
    override fun observeAll(): Flow<List<InstalledSkillPackage>> =
        skillInstallDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(skillId: String): InstalledSkillPackage? =
        skillInstallDao.get(skillId)?.toDomain()

    override suspend fun upsert(pkg: InstalledSkillPackage) {
        skillInstallDao.upsert(pkg.toEntity())
    }

    override suspend fun setEnabled(skillId: String, enabled: Boolean) {
        skillInstallDao.setEnabled(skillId, enabled)
    }

    override suspend fun delete(skillId: String) {
        skillInstallDao.delete(skillId)
    }

    private fun SkillInstallEntity.toDomain() = InstalledSkillPackage(
        skillId = skillId,
        displayName = displayName,
        description = description,
        localPath = localPath,
        version = version,
        permissions = permissions,
        enabled = enabled,
        hasJsRuntime = hasJsRuntime,
        installedAtMillis = installedAtMillis,
    )

    private fun InstalledSkillPackage.toEntity() = SkillInstallEntity(
        skillId = skillId,
        displayName = displayName,
        description = description,
        localPath = localPath,
        version = version,
        permissions = permissions,
        enabled = enabled,
        hasJsRuntime = hasJsRuntime,
        installedAtMillis = installedAtMillis,
    )
}

@Singleton
class ActionHistoryRepositoryImpl @Inject constructor(
    private val actionHistoryDao: ActionHistoryDao,
) : ActionHistoryRepository {
    override suspend fun log(actionId: String, status: String, summary: String) {
        actionHistoryDao.insert(
            ActionHistoryEntity(
                actionId = actionId,
                status = status,
                timestampMillis = System.currentTimeMillis(),
                summary = summary,
            ),
        )
    }
}
