package com.nibbli.nibbligo.core.model

object ModelCatalog {
    const val RECOMMENDED_MODEL_ID = "qwen2.5-1.5b-instruct"
    const val RECOMMENDED_PET_MODEL_ID = RECOMMENDED_MODEL_ID

    val models: List<ModelInfo> = listOf(
        ModelInfo(
            id = "gemma3-1b-it",
            displayName = "Gemma 3 1B IT",
            description = "Compact chat model (~584 MB). Good for Pixel Friend and quick on-device chat. " +
                "Requires Hugging Face sign-in and accepting the Google Gemma license.",
            sizeBytes = 584_417_280L,
            estimatedRamMb = 1536,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = true,
            hfRepoId = "litert-community/Gemma3-1B-IT",
            hfModelFile = "gemma3-1b-it-int4.litertlm",
            hfCommitHash = "42d538a932e8d5b12e6b3b455f5572560bd60b2c",
            preferredAccelerators = listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            maxContextTokens = 1024,
        ),
        ModelInfo(
            id = "functiongemma-270m",
            displayName = "FunctionGemma 270M (Mobile Actions)",
            description = "On-device mobile actions via function calling. ~289 MB. Requires Hugging Face sign-in and accepting the model license.",
            sizeBytes = 288_964_608L,
            estimatedRamMb = 512,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = true,
            hfRepoId = "litert-community/functiongemma-270m-ft-mobile-actions",
            hfModelFile = "mobile_actions_q8_ekv1024.litertlm",
            hfCommitHash = "38942192c9b723af836d489074823ff33d4a3e7a",
            preferredAccelerators = listOf(LiteRtAccelerator.CPU),
            maxContextTokens = 1024,
        ),
        ModelInfo(
            id = "smollm2-360m-instruct",
            displayName = "SmolLM2 360M Instruct",
            description = "Tiny chat model (~374 MB). Lightweight no-login pick for Pixel Friend on low-RAM devices.",
            sizeBytes = 373_719_040L,
            estimatedRamMb = 768,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/SmolLM2-360M-Instruct",
            hfModelFile = "SmolLM2_360M_instruct.litertlm",
            hfCommitHash = "507c99cfe6541ba2bcd84818786f7b025935e5e1",
            preferredAccelerators = listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            maxContextTokens = 1024,
        ),
        ModelInfo(
            id = "qwen2.5-1.5b-instruct",
            displayName = "Qwen 2.5 1.5B Instruct",
            description = "Open-weight instruct model (~1.6 GB). Best all-round pick for nibbliGO — " +
                "strong Pixel Friend dialogue and chat with a 4096-token context window.",
            sizeBytes = 1_597_931_520L,
            estimatedRamMb = 2048,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/Qwen2.5-1.5B-Instruct",
            hfModelFile = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            hfCommitHash = "19edb84c69a0212f29a6ef17ba0d6f278b6a1614",
            preferredAccelerators = listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            maxContextTokens = 4096,
            recommendedForNibbliGo = true,
        ),
        ModelInfo(
            id = "deepseek-r1-distill-qwen-1.5b",
            displayName = "DeepSeek R1 Distill Qwen 1.5B",
            description = "Reasoning-oriented 1.5B model (~1.8 GB). May show chain-of-thought style output.",
            sizeBytes = 1_833_451_520L,
            estimatedRamMb = 2048,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
            hfModelFile = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            hfCommitHash = "e34bb88632342d1f9640bad579a45134eb1cf988",
            preferredAccelerators = listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            maxContextTokens = 4096,
        ),
        ModelInfo(
            id = "gemma-4-e2b-it",
            displayName = "Gemma 4 E2B (LiteRT)",
            description = "Agentic chat with thinking trace. ~2.4 GB download over Wi‑Fi.",
            sizeBytes = 2_588_147_712L,
            estimatedRamMb = 4096,
            modalities = setOf(Modality.TEXT),
            isBundled = false,
            requiresLiteRt = true,
            requiresHfAuth = false,
            hfRepoId = "litert-community/gemma-4-E2B-it-litert-lm",
            hfModelFile = "gemma-4-E2B-it.litertlm",
            hfCommitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
            preferredAccelerators = listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            maxContextTokens = 4000,
        ),
    )

    fun find(modelId: String): ModelInfo? = models.find { it.id == modelId }

    fun displayName(modelId: String): String = find(modelId)?.displayName ?: modelId

    fun approximateSizeLabel(modelId: String): String {
        val mb = (find(modelId)?.sizeBytes ?: 0L) / 1_000_000
        return if (mb >= 1000) "~${mb / 1000}.${(mb % 1000) / 100} GB" else "~$mb MB"
    }

    const val LIGHTWEIGHT_PET_MODEL_ID = "smollm2-360m-instruct"
    private const val LOW_RAM_THRESHOLD_MB = 3072L

    fun recommendedPetModelId(totalRamMb: Long): String =
        if (totalRamMb < LOW_RAM_THRESHOLD_MB) LIGHTWEIGHT_PET_MODEL_ID else RECOMMENDED_PET_MODEL_ID
}
