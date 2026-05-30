package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetGameReference

/** On-device FAQ catalog for Pixel Friend game mechanics. */
object PetGameFaq {
    val entries: List<PetGameFaqEntry> = listOf(
        PetGameFaqEntry(
            id = "care_basics",
            topics = setOf("care", "basics", "play"),
            keywords = setOf(
                "how do i play", "how to play", "take care", "caretaker", "care for",
                "basics", "getting started", "what do i do", "help me play",
            ),
            answer = "Feed Meal or Snack when hungry, Clean when messy, Play or Talk for mood, " +
                "Sleep when tired, Meds when sick, Train for skill. Check my LCD care menu on Home.",
        ),
        PetGameFaqEntry(
            id = "hunger",
            topics = setOf("hunger", "food", "feed"),
            keywords = setOf("hunger", "hungry", "feed", "meal", "snack", "food", "eat", "tummy"),
            answer = "Hunger drops over time. Meal +22 hunger; Snack +8 hunger and big mood boost. " +
                "I get hungry when hunger falls below ${PetGameReference.NEED_HUNGRY_THRESHOLD}. " +
                "Can't Play if too hungry.",
        ),
        PetGameFaqEntry(
            id = "hygiene",
            topics = setOf("hygiene", "clean", "mess"),
            keywords = setOf("hygiene", "clean", "mess", "messy", "dirty", "sparkle", "room"),
            answer = "Hygiene drops while awake. Low hygiene can make a mess; Clean clears it and +25 hygiene. " +
                "Mess + low hygiene can make me sick.",
        ),
        PetGameFaqEntry(
            id = "energy",
            topics = setOf("energy", "sleep", "tired"),
            keywords = setOf("energy", "sleep", "tired", "nap", "wake", "rest", "zzz"),
            answer = "Energy drops while awake and recovers while sleeping. Sleep on the LCD menu; Wake when ready. " +
                "Tired when energy below ${PetGameReference.NEED_TIRED_ENERGY_THRESHOLD}.",
        ),
        PetGameFaqEntry(
            id = "sickness",
            topics = setOf("sick", "medicine", "health"),
            keywords = setOf("sick", "medicine", "meds", "health", "ill", "feel bad"),
            answer = "Mess + low hygiene can cause sickness. Use Meds when sick (+30 health). " +
                "Meds when healthy hurts trust and health.",
        ),
        PetGameFaqEntry(
            id = "trust",
            topics = setOf("trust", "bond"),
            keywords = setOf("trust", "bond", "bonding", "believe"),
            answer = "Trust grows from Talk, Train, pet taps, and good care. Using Assist chat or Agent " +
                "can also boost trust. Low trust slows cosmetic unlocks.",
        ),
        PetGameFaqEntry(
            id = "skill",
            topics = setOf("skill", "train"),
            keywords = setOf("skill", "train", "training", "smarter", "learn"),
            answer = "Train on the LCD menu (+6 skill, costs energy). Minigame wins and Assist work also raise skill. " +
                "Higher skill unlocks Looks cosmetics.",
        ),
        PetGameFaqEntry(
            id = "needs_priority",
            topics = setOf("needs", "priority", "badge"),
            keywords = setOf("need", "priority", "badge", "header", "what need", "urgent"),
            answer = "Needs checked in order: Sick, Hungry, Dirty, Tired, Unhappy, Lonely (8h+ no visit). " +
                "Home header badges show what's urgent.",
        ),
        PetGameFaqEntry(
            id = "evolution",
            topics = setOf("evolution", "grow", "stage"),
            keywords = setOf(
                "evolve", "evolution", "grow", "stage", "hatch", "egg", "baby", "child", "teen", "adult",
            ),
            answer = PetGameReference.formatEvolutionSummary() +
                " Care score rises when hunger/mood/hygiene stay above " +
                "${PetGameReference.CARE_SCORE_TICK_THRESHOLD} and you interact (+${PetGameReference.CARE_SCORE_PER_INTERACTION} per action).",
        ),
        PetGameFaqEntry(
            id = "death",
            topics = setOf("death", "rebirth", "egg"),
            keywords = setOf("die", "death", "dead", "fade", "rebirth", "new egg", "hatch again", "gone"),
            answer = "If hunger below ${PetGameReference.CRITICAL_HUNGER_THRESHOLD} or health below " +
                "${PetGameReference.CRITICAL_HEALTH_THRESHOLD} for 4 hours, I fade away. " +
                "Tap Hatch egg to start fresh; some memory carries over.",
        ),
        PetGameFaqEntry(
            id = "cosmetics",
            topics = setOf("looks", "cosmetic", "unlock"),
            keywords = setOf("looks", "cosmetic", "collar", "patch", "aura", "unlock", "equip", "wear"),
            answer = "Looks unlock when skill AND trust hit thresholds: " +
                PetGameReference.formatCosmeticsSummary() +
                ". Open Looks on Home; highest new unlock may auto-equip.",
        ),
        PetGameFaqEntry(
            id = "minigame",
            topics = setOf("minigame", "catch"),
            keywords = setOf("minigame", "catch", "treat", "game", "play catch", "score"),
            answer = "Treat Catch: ${PetGameReference.MINIGAME_DURATION_SECONDS}s round, score " +
                "${PetGameReference.MINIGAME_WIN_SCORE}+ to win. Win gives +20 mood and +2 skill. " +
                "Open via Catch quick action or ask to play.",
        ),
        PetGameFaqEntry(
            id = "notifications",
            topics = setOf("notification", "background", "tick"),
            keywords = setOf("notification", "notify", "background", "tick", "remind", "alert"),
            answer = "I tick every ~${PetGameReference.TICK_INTERVAL_MINUTES} minutes in the background. " +
                "You get a notification when I'm Hungry, Dirty, Tired, Sick, or Unhappy (not just lonely).",
        ),
        PetGameFaqEntry(
            id = "talk_model",
            topics = setOf("model", "llm", "talk"),
            keywords = setOf(
                "model", "download", "llm", "talk", "on-device", "smollm", "install model", "brain",
            ),
            answer = "Talk and voice need a downloaded Pixel Friend model (Manage → Models). " +
                "SmolLM2 360M is fast and needs no Hugging Face login. Care works without any model.",
        ),
        PetGameFaqEntry(
            id = "assist_boost",
            topics = setOf("assist", "agent", "boost"),
            keywords = setOf("assist", "agent", "chat", "boost", "prompt lab", "bonus"),
            answer = "Using Assist chat, Agent, Prompt Lab, or completing actions can raise my mood, trust, " +
                "skill, or curiosity. Settings can toggle comments on agent work.",
        ),
        PetGameFaqEntry(
            id = "care_actions",
            topics = setOf("actions", "lcd", "menu"),
            keywords = setOf("lcd", "menu", "meal", "play", "clean", "meds", "train", "actions", "button"),
            answer = "LCD care menu: Meal, Talk, Snack, Play, Clean, Meds, Sleep/Wake, Train. " +
                "Eggs can only Talk until they hatch.",
        ),
        PetGameFaqEntry(
            id = "mood_pulse",
            topics = setOf("mood", "ambient", "pulse"),
            keywords = setOf("mood pulse", "ambient", "spontaneous", "thought", "home screen"),
            answer = "Mood pulse (Settings) makes me say spontaneous lines on Home every 60–120s. " +
                "Needs a Pixel Friend model. Off / Normal / Quiet modes available.",
        ),
        PetGameFaqEntry(
            id = "personality",
            topics = setOf("personality", "settings"),
            keywords = setOf("personality", "playful", "calm", "curious", "character"),
            answer = "Personality in Settings (Playful, Calm, Curious) shapes how I talk — not hunger or evolution math.",
        ),
    )

    fun findById(id: String): PetGameFaqEntry? = entries.firstOrNull { it.id == id }
}
