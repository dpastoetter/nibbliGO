package com.nibbli.nibbligo.core.model

object ModelCatalog {
    val models: List<ModelInfo> = listOf(
        ModelInfo(
            id = "nibbli-fast",
            displayName = "nibbli Fast",
            description = "Lightweight text model for chat and prompts. Great for everyday tasks.",
            sizeBytes = 450_000_000L,
            estimatedRamMb = 512,
            modalities = setOf(Modality.TEXT),
        ),
        ModelInfo(
            id = "nibbli-vision",
            displayName = "nibbli Vision",
            description = "Multimodal model for chat plus image understanding and description.",
            sizeBytes = 1_200_000_000L,
            estimatedRamMb = 1024,
            modalities = setOf(Modality.TEXT, Modality.VISION),
        ),
        ModelInfo(
            id = "nibbli-scribe",
            displayName = "nibbli Scribe",
            description = "Audio transcription and summarization on-device.",
            sizeBytes = 800_000_000L,
            estimatedRamMb = 768,
            modalities = setOf(Modality.TEXT, Modality.AUDIO),
        ),
        ModelInfo(
            id = "gemma-4-e2b-it",
            displayName = "Gemma 4 E2B (LiteRT)",
            description = "Agentic chat with tool calling. ~2.4 GB download over Wi‑Fi.",
            sizeBytes = 2_588_147_712L,
            estimatedRamMb = 4096,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/gemma-4-E2B-it-litert-lm",
            hfModelFile = "gemma-4-E2B-it.litertlm",
            hfCommitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
        ),
        ModelInfo(
            id = "functiongemma-270m",
            displayName = "FunctionGemma 270M (Mobile Actions)",
            description = "On-device mobile actions via function calling. ~289 MB download.",
            sizeBytes = 288_964_608L,
            estimatedRamMb = 512,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/functiongemma-270m-ft-mobile-actions",
            hfModelFile = "mobile_actions_q8_ekv1024.litertlm",
            hfCommitHash = "38942192c9b723af836d489074823ff33d4a3e7a",
        ),
    )

    fun find(modelId: String): ModelInfo? = models.find { it.id == modelId }
}
