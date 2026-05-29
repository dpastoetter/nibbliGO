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
    /** Hugging Face repo id, e.g. litert-community/gemma-4-E2B-it-litert-lm */
    val hfRepoId: String? = null,
    /** File name inside the repo, e.g. gemma-4-E2B-it.litertlm */
    val hfModelFile: String? = null,
    val hfCommitHash: String? = null,
    /** Requires Hugging Face sign-in and accepting the repo license before download. */
    val requiresHfAuth: Boolean = false,
) {
    fun resolveDownloadUrl(): String? {
        downloadUrl?.let { return it }
        val repo = hfRepoId ?: return null
        val file = hfModelFile ?: return null
        val version = hfCommitHash ?: "main"
        return "https://huggingface.co/$repo/resolve/$version/$file?download=true"
    }

    fun hfRepoUrl(): String? = hfRepoId?.let { "https://huggingface.co/$it" }

    fun localFileName(): String = hfModelFile ?: "$id.litertlm"
}

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
