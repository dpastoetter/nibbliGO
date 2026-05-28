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
            description = "Agentic chat with tool calling and thinking trace. Downloads a LiteRT bundle when Wi‑Fi allows.",
            sizeBytes = 2_600_000_000L,
            estimatedRamMb = 4096,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            downloadUrl = "https://huggingface.co/google/gemma-4-e2b-it/resolve/main/README.md",
        ),
        ModelInfo(
            id = "functiongemma-270m",
            displayName = "FunctionGemma 270M",
            description = "Mobile actions and device control via function calling.",
            sizeBytes = 270_000_000L,
            estimatedRamMb = 512,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            downloadUrl = "https://huggingface.co/google/functiongemma-270m/resolve/main/README.md",
        ),
    )

    fun find(modelId: String): ModelInfo? = models.find { it.id == modelId }
}
