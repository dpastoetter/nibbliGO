package com.nibbli.nibbligo.core.model

/** How Assist Chat applies model instructions (Home talk always records Pixel Friend prompts). */
enum class ChatPromptMode {
    /** Generic chat with [GenerationParams.systemPrompt] only. */
    PURE_LLM,
    /** Pixel Friend system rules + per-turn pet context (same path as Home talk). */
    PIXEL_FRIEND,
}
