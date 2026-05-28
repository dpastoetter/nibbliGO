package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.storage.work.ModelDownloadWorker
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val modelInstallDao: ModelInstallDao,
) : ModelRepository {

  private val modelsDir: File
    get() = File(context.filesDir, "models").also { it.mkdirs() }

  override fun observeCatalog(): Flow<List<ModelInfo>> =
    modelInstallDao.observeAll().map { ModelCatalog.models }

  override fun observeInstalled(): Flow<List<InstalledModel>> =
    modelInstallDao.observeAll().map { list -> list.map { it.toDomain() } }

  override suspend fun getCatalog(): List<ModelInfo> = ModelCatalog.models

  override suspend fun getInstalled(): List<InstalledModel> =
    modelInstallDao.getAll().map { it.toDomain() }

  override suspend fun isInstalled(modelId: String): Boolean =
    modelInstallDao.get(modelId) != null

  override suspend fun install(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val info = ModelCatalog.find(modelId)
      ?: return@withContext Result.failure(IllegalArgumentException("Unknown model: $modelId"))
    if (info.requiresLiteRt) {
      val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
        .setInputData(ModelDownloadWorker.input(modelId))
        .build()
      WorkManager.getInstance(context).enqueue(request)
      return@withContext Result.success(Unit)
    }
    val modelFile = File(modelsDir, "$modelId.nibbli")
    if (!modelFile.exists()) {
      modelFile.writeText("fake-model-placeholder:${info.id}")
    }
    modelInstallDao.insert(
      com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity(
        modelId = modelId,
        localPath = modelFile.absolutePath,
        installedAtMillis = System.currentTimeMillis(),
        sizeBytes = info.sizeBytes,
      ),
    )
    Result.success(Unit)
  }

  override suspend fun uninstall(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
    modelInstallDao.get(modelId)?.localPath?.let { File(it).delete() }
    modelInstallDao.delete(modelId)
    Result.success(Unit)
  }

  override suspend fun getInstalledModelIds(): List<String> =
    modelInstallDao.getAll().map { it.modelId }
}
