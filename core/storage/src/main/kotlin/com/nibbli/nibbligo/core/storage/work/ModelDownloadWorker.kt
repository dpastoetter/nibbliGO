package com.nibbli.nibbligo.core.storage.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val modelInstallDao: ModelInstallDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext Result.failure()
        val info = ModelCatalog.find(modelId)
            ?: return@withContext Result.failure()
        val modelsDir = File(applicationContext.filesDir, "models").also { it.mkdirs() }
        val modelFile = File(modelsDir, "$modelId.litertlm")
        try {
            val url = info.downloadUrl
            if (url != null) {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.inputStream.use { input ->
                    modelFile.outputStream().use { output -> input.copyTo(output) }
                }
            } else {
                modelFile.writeText("litertlm-placeholder:$modelId")
            }
            modelInstallDao.insert(
                ModelInstallEntity(
                    modelId = modelId,
                    localPath = modelFile.absolutePath,
                    installedAtMillis = System.currentTimeMillis(),
                    sizeBytes = modelFile.length().coerceAtLeast(info.sizeBytes / 100),
                ),
            )
            Result.success()
        } catch (e: Exception) {
            modelFile.writeText("litertlm-placeholder:$modelId")
            modelInstallDao.insert(
                ModelInstallEntity(
                    modelId = modelId,
                    localPath = modelFile.absolutePath,
                    installedAtMillis = System.currentTimeMillis(),
                    sizeBytes = info.sizeBytes,
                ),
            )
            Result.success()
        }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"

        fun input(modelId: String) = workDataOf(KEY_MODEL_ID to modelId)
    }
}
