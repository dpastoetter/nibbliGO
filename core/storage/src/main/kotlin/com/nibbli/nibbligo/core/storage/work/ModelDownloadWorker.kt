package com.nibbli.nibbligo.core.storage.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nibbli.nibbligo.core.hf.download.HfDownloadException
import com.nibbli.nibbligo.core.hf.download.HfFileDownloader
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val modelInstallDao: ModelInstallDao,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext Result.failure()
        val info = ModelCatalog.find(modelId)
            ?: return@withContext Result.failure()
        val modelsDir = File(applicationContext.filesDir, "models").also { it.mkdirs() }
        val modelFile = File(modelsDir, info.localFileName())
        val url = info.resolveDownloadUrl()
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "No download URL for $modelId"),
            )

        setForeground(createForegroundInfo(modelId, 0))

        try {
            val token = huggingFaceAuthRepository.getAccessToken()
            try {
                HfFileDownloader.download(url, modelFile, token)
            } catch (first: HfDownloadException) {
                if (first.httpCode == 401 && !token.isNullOrBlank()) {
                    HfFileDownloader.download(url, modelFile, accessToken = null)
                } else {
                    throw first
                }
            }

            val minBytes = (info.sizeBytes / 50).coerceAtLeast(500_000L)
            if (modelFile.length() < minBytes) {
                modelFile.delete()
                error(
                    "Downloaded file too small (${modelFile.length()} bytes). " +
                        "If the repo is gated, sign in or add a token in Settings.",
                )
            }
            modelInstallDao.insert(
                ModelInstallEntity(
                    modelId = modelId,
                    localPath = modelFile.absolutePath,
                    installedAtMillis = System.currentTimeMillis(),
                    sizeBytes = modelFile.length(),
                ),
            )
            Log.i(TAG, "Installed $modelId (${modelFile.length()} bytes)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $modelId", e)
            modelFile.delete()
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "download failed")))
        }
    }

    private fun createForegroundInfo(modelId: String, progress: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading model")
            .setContentText(modelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(
            modelId.hashCode(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Model downloads",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val TAG = "ModelDownloadWorker"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_ERROR = "error"
        const val WORK_TAG = "hf_litert_download"
        private const val CHANNEL_ID = "model_download"

        fun input(modelId: String) = workDataOf(KEY_MODEL_ID to modelId)
    }
}
