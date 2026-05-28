package com.nibbli.nibbligo.core.model

data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val estimatedRamMb: Int,
    val modalities: Set<Modality>,
    val isBundled: Boolean = true,
    val downloadUrl: String? = null,
    val requiresLiteRt: Boolean = false,
)

data class InstalledModel(
    val modelId: String,
    val localPath: String,
    val installedAtMillis: Long,
    val sizeBytes: Long,
)

data class ModelCapabilities(
    val modelId: String,
    val supportsChat: Boolean,
    val supportsVision: Boolean,
    val supportsAudio: Boolean,
    val supportsStreaming: Boolean,
    val supportsToolCalling: Boolean = false,
    val supportsThinkingTrace: Boolean = false,
)
