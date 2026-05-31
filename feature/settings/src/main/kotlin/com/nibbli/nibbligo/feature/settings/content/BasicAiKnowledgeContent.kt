package com.nibbli.nibbligo.feature.settings.content

object BasicAiKnowledgeContent {
    val sections: List<FaqSection> = listOf(
        FaqSection(
            title = "Generative AI basics",
            items = listOf(
                FaqItem(
                    question = "What is a large language model (LLM)?",
                    answer = "An LLM is a neural network trained on text to predict likely next words. " +
                        "It can answer questions, chat, and follow instructions — but it does not " +
                        "truly “understand” or browse the web unless given tools.",
                ),
                FaqItem(
                    question = "What are hallucinations?",
                    answer = "Models sometimes invent plausible-sounding facts, names, or steps that are " +
                        "wrong. Always verify important information (health, legal, financial, safety).",
                ),
                FaqItem(
                    question = "Can I trust everything nibbli says?",
                    answer = "No. Treat replies as suggestions or companionship, not professional advice. " +
                        "You are responsible for decisions you make based on model output.",
                ),
            ),
        ),
        FaqSection(
            title = "On-device vs cloud",
            items = listOf(
                FaqItem(
                    question = "Why run AI on my phone?",
                    answer = "On-device inference keeps your Pixel Friend chat local, works offline " +
                        "after models are downloaded, and avoids sending prompts to a remote server.",
                ),
                FaqItem(
                    question = "What are the trade-offs of small on-device models?",
                    answer = "Phone models are smaller and faster than cloud giants but may be less " +
                        "accurate, shorter in context, or weaker at complex reasoning.",
                ),
                FaqItem(
                    question = "What is GPU / CPU / NPU?",
                    answer = "These are hardware backends LiteRT can use. GPU is often fastest on " +
                        "Pixel phones; CPU is universal; NPU uses dedicated AI chips when available.",
                ),
            ),
        ),
        FaqSection(
            title = "Using AI responsibly",
            items = listOf(
                FaqItem(
                    question = "What should I not use nibbliGO for?",
                    answer = "Do not use the app for anything illegal or harmful. Do not rely on it for " +
                        "emergency, medical, legal, or financial decisions without expert verification.",
                ),
                FaqItem(
                    question = "Why was I asked to accept terms at onboarding?",
                    answer = "You acknowledged that GenAI can err, that you will use the app lawfully, " +
                        "and that the nibbliGO team has no liability for model mistakes or actions you take.",
                ),
            ),
        ),
    )
}
