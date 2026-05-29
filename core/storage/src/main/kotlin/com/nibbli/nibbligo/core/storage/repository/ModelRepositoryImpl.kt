package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nibbli.nibbligo.core.domain.download.HfModelDownloadScheduler
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelInstallDao: ModelInstallDao,
    private val hfDownloadScheduler: HfModelDownloadScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
) : ModelRepository {

    override fun observeCatalog(): Flow<List<ModelInfo>> =
        modelInstallDao.observeAll().map { ModelCatalog.models }

    override fun observeInstalled(): Flow<List<InstalledModel>> =
        modelInstallDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCatalog(): List<ModelInfo> = ModelCatalog.models

    override suspend fun getInstalled(): List<InstalledModel> {
        purgeInvalidInstalls()
        return modelInstallDao.getAll().map { it.toDomain() }
    }

    override suspend fun isInstalled(modelId: String): Boolean {
        purgeInvalidInstalls()
        return modelInstallDao.get(modelId) != null
    }

    override suspend fun install(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val info = ModelCatalog.find(modelId)
            ?: return@withContext Result.failure(IllegalArgumentException("Unknown model: $modelId"))
        if (!userPreferencesRepository.allowDownloads.first()) {
            return@withContext Result.failure(
                IllegalStateException("Model downloads are disabled in Settings."),
            )
        }
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(
                IllegalStateException("No network connection. Connect to download models."),
            )
        }
        if (info.requiresHfAuth) {
            val token = huggingFaceAuthRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                val repo = info.hfRepoUrl() ?: info.hfRepoId.orEmpty()
                return@withContext Result.failure(
                    IllegalStateException(
                        "Sign in to Hugging Face under Settings (or paste an access token), " +
                            "accept the model license at $repo, then try again.",
                    ),
                )
            }
        }
        hfDownloadScheduler.enqueueLiteRtModelDownload(modelId)
        Result.success(Unit)
    }

    override suspend fun uninstall(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        modelInstallDao.get(modelId)?.localPath?.let { File(it).delete() }
        modelInstallDao.delete(modelId)
        Result.success(Unit)
    }

    override suspend fun getInstalledModelIds(): List<String> =
        getInstalled().map { it.modelId }

    private suspend fun purgeInvalidInstalls() {
        modelInstallDao.getAll().forEach { entity ->
            val file = File(entity.localPath)
            val valid = file.isFile &&
                file.name.endsWith(".litertlm", ignoreCase = true) &&
                file.length() >= MIN_LITERT_BYTES
            if (!valid) {
                if (file.exists()) file.delete()
                modelInstallDao.delete(entity.modelId)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        private const val MIN_LITERT_BYTES = 1_000_000L
    }
}
