package com.nibbli.nibbligo.core.domain.model

import java.io.File

interface InstalledModelPathResolver {
    fun resolveFile(modelId: String): File?

    /** Reload path cache from Room (call after install/uninstall or at app start). */
    suspend fun refreshCache(modelId: String? = null)
}
