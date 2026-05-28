package com.nibbli.nibbligo.core.domain.model

import java.io.File

interface InstalledModelPathResolver {
    fun resolveFile(modelId: String): File?
}
