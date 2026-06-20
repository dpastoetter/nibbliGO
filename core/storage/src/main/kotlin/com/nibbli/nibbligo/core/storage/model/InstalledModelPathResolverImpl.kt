package com.nibbli.nibbligo.core.storage.model

import android.content.Context
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledModelPathResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelInstallDao: ModelInstallDao,
) : InstalledModelPathResolver {
    private val cache = ConcurrentHashMap<String, File>()

    override fun resolveFile(modelId: String): File? {
        cache[modelId]?.takeIf { it.isFile && it.length() > 0 }?.let { return it }
        return catalogFallback(modelId)
    }

    override suspend fun refreshCache(modelId: String?) {
        if (modelId != null) {
            refreshSingle(modelId)
            return
        }
        val installed = modelInstallDao.getAll()
        cache.clear()
        installed.forEach { entity ->
            val file = File(entity.localPath)
            if (file.isFile && file.length() > 0) {
                cache[entity.modelId] = file
            }
        }
    }

    private suspend fun refreshSingle(modelId: String) {
        val entity = modelInstallDao.get(modelId)
        if (entity == null) {
            cache.remove(modelId)
            return
        }
        val file = File(entity.localPath)
        if (file.isFile && file.length() > 0) {
            cache[modelId] = file
        } else {
            cache.remove(modelId)
        }
    }

    private fun catalogFallback(modelId: String): File? {
        val catalog = ModelCatalog.find(modelId) ?: return null
        val modelsDir = File(context.filesDir, "models")
        return File(modelsDir, catalog.localFileName()).takeIf { it.isFile && it.length() > 0 }
    }
}
