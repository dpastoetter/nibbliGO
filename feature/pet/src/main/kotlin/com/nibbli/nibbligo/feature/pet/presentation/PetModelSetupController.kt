package com.nibbli.nibbligo.feature.pet.presentation

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** Observes model setup / download state for Home onboarding banner. */
@Singleton
class PetModelSetupController @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository,
) {
    fun observeDownloadWork(workManager: WorkManager): Flow<List<WorkInfo>> =
        runCatching {
            workManager.getWorkInfosByTagFlow(com.nibbli.nibbligo.core.storage.work.ModelDownloadWorker.WORK_TAG)
        }.getOrElse { flowOf(emptyList()) }

    suspend fun recommendedPetModelId(contextRamMb: Long): String =
        com.nibbli.nibbligo.core.model.ModelCatalog.recommendedPetModelId(contextRamMb)

    fun observeInstalled(): Flow<List<InstalledModel>> = modelRepository.observeInstalled()

    val onboardingCompleted = userPreferencesRepository.onboardingCompleted

    val modelSetupPromptDismissed = userPreferencesRepository.modelSetupPromptDismissed
}
