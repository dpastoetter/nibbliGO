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
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.hf.download.HfDownloadException
import com.nibbli.nibbligo.core.hf.download.HfFileDownloader
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val modelInstallDao: ModelInstallDao,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val installedModelPathResolver: InstalledModelPathResolver,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext failure("Missing model id in download request.")
        val info = ModelCatalog.find(modelId)
            ?: return@withContext failure("Unknown model: $modelId")
        val modelsDir = File(applicationContext.filesDir, "models").also { it.mkdirs() }
        val modelFile = File(modelsDir, info.localFileName())
        val url = info.resolveDownloadUrl()
            ?: return@withContext failure("No download URL for $modelId")

        val tempFile = File(modelFile.parentFile, "${modelFile.name}.download")
        val isResuming = tempFile.isFile && tempFile.length() > 0L

        setForeground(createForegroundInfo(modelId, 0))
        setProgress(workDataOf(KEY_PROGRESS to 0, KEY_IS_RESUMING to isResuming))

        try {
            val token = huggingFaceAuthRepository.getAccessToken()
            try {
                downloadWithProgress(url, modelFile, token, modelId)
            } catch (first: HfDownloadException) {
                if (first.httpCode == 401 && !token.isNullOrBlank() && !info.requiresHfAuth) {
                    downloadWithProgress(url, modelFile, accessToken = null, modelId = modelId)
                } else {
                    throw gatedDownloadError(first, info)
                }
            }

            setProgress(workDataOf(KEY_PROGRESS to 100))
            setForeground(createForegroundInfo(modelId, 100))

            val minBytes = (info.sizeBytes / 50).coerceAtLeast(500_000L)
            if (modelFile.length() < minBytes) {
                modelFile.delete()
                File(modelFile.parentFile, "${modelFile.name}.download").delete()
                return@withContext failure(
                    "Downloaded file too small (${modelFile.length()} bytes). " +
                        "If the repo is gated, add a Hugging Face token in Settings.",
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
            if (userPreferencesRepository.defaultModelId.first() == null) {
                userPreferencesRepository.setDefaultModelId(modelId)
            }
            if (userPreferencesRepository.petModelId.first() == null &&
                modelId != "functiongemma-270m"
            ) {
                userPreferencesRepository.setPetModelId(modelId)
            }
            installedModelPathResolver.refreshCache(modelId)
            Log.i(TAG, "Installed $modelId (${modelFile.length()} bytes)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $modelId", e)
            modelFile.delete()
            failure(e.message ?: "Download failed")
        }
    }

    private suspend fun downloadWithProgress(
        url: String,
        modelFile: File,
        accessToken: String?,
        modelId: String,
    ) {
        var lastReportedPercent = -1
        var lastForegroundPercent = -1
        var lastReportTimeMs = 0L

        HfFileDownloader.download(url, modelFile, accessToken) { bytesRead, totalBytes ->
            val percent = if (totalBytes > 0L) {
                ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 99)
            } else {
                -1
            }
            val now = System.currentTimeMillis()
            val percentChanged = percent >= 0 && percent != lastReportedPercent
            val timeElapsed = now - lastReportTimeMs >= PROGRESS_MIN_INTERVAL_MS
            if (!percentChanged && !timeElapsed) return@download

            lastReportTimeMs = now
            if (percent >= 0) {
                lastReportedPercent = percent
            }
            val progressValue = percent.coerceAtLeast(0)
            setProgressAsync(workDataOf(KEY_PROGRESS to progressValue))

            if (percent >= 0 && percent != lastForegroundPercent) {
                lastForegroundPercent = percent
                setForegroundAsync(createForegroundInfo(modelId, percent))
            } else if (percent < 0 && timeElapsed) {
                setForegroundAsync(createForegroundInfo(modelId, 0))
            }
        }
    }

    private fun failure(message: String): Result =
        Result.failure(workDataOf(KEY_ERROR to message))

    private fun gatedDownloadError(error: HfDownloadException, info: ModelInfo): HfDownloadException {
        if (error.httpCode != 401 && error.httpCode != 403) return error
        val repo = info.hfRepoUrl() ?: "huggingface.co/${info.hfRepoId.orEmpty()}"
        return when (error.httpCode) {
            401 -> HfDownloadException(
                httpCode = 401,
                message = "Hugging Face sign-in required. Open Settings to sign in or paste a token, " +
                    "accept the license at $repo, then retry.",
            )
            403 -> HfDownloadException(
                httpCode = 403,
                message = "Access denied. Accept the model license at $repo and use a token with read + gated-repos scope.",
            )
            else -> error
        }
    }

    private fun createForegroundInfo(modelId: String, progress: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading model")
            .setContentText(
                if (progress > 0) "$modelId — $progress%" else "$modelId — starting…",
            )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.coerceIn(0, 100), progress == 0)
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
        private const val PROGRESS_MIN_INTERVAL_MS = 500L
        const val KEY_MODEL_ID = "model_id"
        const val KEY_ERROR = "error"
        const val KEY_PROGRESS = "progress"
        const val KEY_IS_RESUMING = "is_resuming"
        const val WORK_TAG = "hf_litert_download"

        private const val CHANNEL_ID = "model_download"

        fun input(modelId: String) = workDataOf(KEY_MODEL_ID to modelId)
    }
}
