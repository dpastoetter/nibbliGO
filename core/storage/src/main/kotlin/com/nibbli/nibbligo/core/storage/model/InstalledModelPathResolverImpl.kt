package com.nibbli.nibbligo.core.storage.model

import android.content.Context
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledModelPathResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelInstallDao: ModelInstallDao,
) : InstalledModelPathResolver {
    override fun resolveFile(modelId: String): File? {
        val fromDb = runBlocking { modelInstallDao.get(modelId)?.localPath }
            ?.let { File(it) }
            ?.takeIf { it.isFile && it.length() > 0 }
        if (fromDb != null) return fromDb
        val catalog = ModelCatalog.find(modelId) ?: return null
        val modelsDir = File(context.filesDir, "models")
        return File(modelsDir, catalog.localFileName()).takeIf { it.isFile && it.length() > 0 }
    }
}
