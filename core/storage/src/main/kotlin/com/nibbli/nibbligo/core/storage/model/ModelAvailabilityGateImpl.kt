package com.nibbli.nibbligo.core.storage.model

import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelAvailabilityGateImpl @Inject constructor(
    private val modelRepository: ModelRepository,
    private val installedModelPathResolver: InstalledModelPathResolver,
) : ModelAvailabilityGate {

    override suspend fun hasUsableModel(): Boolean = firstUsableModelId() != null

    override suspend fun firstUsableModelId(): String? =
        modelRepository.getInstalled()
            .map { it.modelId }
            .firstOrNull { modelId ->
                val file = installedModelPathResolver.resolveFile(modelId)
                file != null &&
                    file.name.endsWith(".litertlm", ignoreCase = true) &&
                    file.length() >= MIN_LITERT_BYTES
            }

    companion object {
        private const val MIN_LITERT_BYTES = 1_000_000L
    }
}
