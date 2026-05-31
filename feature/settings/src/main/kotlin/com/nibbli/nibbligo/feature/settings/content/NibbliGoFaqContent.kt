package com.nibbli.nibbligo.feature.settings.content

object NibbliGoFaqContent {
    val sections: List<FaqSection> = listOf(
        FaqSection(
            title = "Privacy & on-device AI",
            items = listOf(
                FaqItem(
                    question = "Does nibbliGO send my chats to the cloud?",
                    answer = "Pixel Friend talk on Home and Chat runs on your phone with LiteRT. " +
                        "Your messages are not sent to a cloud LLM for inference. Network is used " +
                        "for Hugging Face model downloads and optional OAuth only.",
                ),
                FaqItem(
                    question = "What does the model badge on Home show?",
                    answer = "After warm load, the badge shows your active Pixel Friend model and " +
                        "the LiteRT backend in use (GPU, CPU, or NPU). Tap the refresh icon beside " +
                        "it to unload and reload the model from disk.",
                ),
            ),
        ),
        FaqSection(
            title = "Models & performance",
            items = listOf(
                FaqItem(
                    question = "Which model should I install first?",
                    answer = "Qwen 2.5 1.5B Instruct is recommended for Pixel Friend talk (~1.6 GB, " +
                        "no sign-in). SmolLM2 360M is lighter for emulators and quick testing.",
                ),
                FaqItem(
                    question = "Why does my emulator show CPU instead of GPU?",
                    answer = "Android emulators usually run LiteRT on CPU. On a physical Pixel, Auto " +
                        "tries GPU (OpenCL) first. Check Manage → Benchmark for the active backend.",
                ),
                FaqItem(
                    question = "What is warm load?",
                    answer = "When you open Home, nibbliGO preloads your Pixel Friend model so the " +
                        "first reply is faster. You may see “Warming up model…” briefly.",
                ),
            ),
        ),
        FaqSection(
            title = "Home, Chat & Agent",
            items = listOf(
                FaqItem(
                    question = "Are Home talk and Chat the same conversation?",
                    answer = "Yes. Typed messages on Home, voice “Talk to me”, and the Chat tab share " +
                        "one Pixel Friend thread stored on device.",
                ),
                FaqItem(
                    question = "What is Agent Chat?",
                    answer = "Agent uses FunctionGemma for phone tools (email drafts, calendar, " +
                        "flashlight, etc.) with confirm-before-run. It is separate from Pixel Friend talk.",
                ),
                FaqItem(
                    question = "What are memory proposals?",
                    answer = "After talk, nibbli may ask “Remember this?” You approve before anything " +
                        "is saved to Manage → Companion memory.",
                ),
            ),
        ),
        FaqSection(
            title = "Pixel Friend game help",
            items = listOf(
                FaqItem(
                    question = "How do I take care of nibbli?",
                    answer = "Use the P1 LCD care menu: Feed Meal or Snack when hungry, Clean when messy, " +
                        "Play or Talk for mood, Sleep when tired, Meds when sick, Train for skill.",
                ),
                FaqItem(
                    question = "What are LCD Items?",
                    answer = "Care menu → Items lets you equip wearables, scenes, and floor props with " +
                        "live preview on the LCD. Unlocks come from care score, evolution, arcade wins, " +
                        "and daily quests.",
                ),
                FaqItem(
                    question = "How do visits work?",
                    answer = "Home → Visit lets you share or scan a QR postcard. A friend’s nibbli " +
                        "appears on your LCD for 24 hours — local only, no server.",
                ),
            ),
        ),
    )
}
