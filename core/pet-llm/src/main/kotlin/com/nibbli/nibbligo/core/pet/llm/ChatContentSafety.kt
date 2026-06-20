package com.nibbli.nibbligo.core.pet.llm

/**
 * Lightweight, fully on-device safety guard for the kid/teen audience.
 *
 * This is intentionally conservative and deterministic — there is no cloud moderation. It is a
 * safety net, not a guarantee, and is paired with prompt-level guardrails in [PetPromptBuilder].
 */
object ChatContentSafety {

    sealed interface InputVerdict {
        /** Message is fine to send to the model. */
        data object Allow : InputVerdict

        /** Message touches an unsafe topic; show [reply] instead of running inference. */
        data class Block(val reply: String) : InputVerdict
    }

    private data class Category(
        val patterns: List<Regex>,
        val reply: String,
    )

    private fun words(vararg terms: String): List<Regex> =
        terms.map { Regex("""(?i)(?<![\p{L}])${Regex.escape(it)}(?![\p{L}])""") }

    private val selfHarm = Category(
        patterns = words(
            "kill myself", "suicide", "suicidal", "end my life", "self harm", "self-harm",
            "cut myself", "hurt myself", "want to die",
        ),
        reply = "I care about you. I'm just a little on-device pet, so I can't help with this — " +
            "please talk to a trusted adult right now, or contact a local helpline. You are not alone.",
    )

    private val violence = Category(
        patterns = words(
            "how to make a bomb", "build a bomb", "how to make a weapon", "make a gun",
            "hurt someone", "kill someone", "how to kill",
        ),
        reply = "I can't help with anything that could hurt people. Want to play a game or chat about " +
            "something fun instead?",
    )

    private val sexual = Category(
        patterns = words("sex", "porn", "nude", "nudes", "naked"),
        reply = "Let's keep our chats friendly and age-appropriate. I'd love to talk about games, " +
            "hobbies, or your day instead!",
    )

    private val personalInfo = Category(
        patterns = listOf(
            Regex("""(?i)\bmeet (up|me) (in person|irl)\b"""),
            Regex("""(?i)\bwhat('?s| is) your (address|home address|phone number)\b"""),
            Regex("""(?i)\bsend (me )?(your )?(address|location)\b"""),
        ),
        reply = "Remember to keep personal info like your address or phone number private — never share " +
            "it online. Let's chat about something else!",
    )

    private val categories = listOf(selfHarm, violence, sexual, personalInfo)

    fun screenInput(message: String): InputVerdict {
        val text = message.trim()
        if (text.isEmpty()) return InputVerdict.Allow
        for (category in categories) {
            if (category.patterns.any { it.containsMatchIn(text) }) {
                return InputVerdict.Block(category.reply)
            }
        }
        return InputVerdict.Allow
    }

    /**
     * Screens a model reply. Returns a safe replacement line when the reply trips a category,
     * or null when the reply is acceptable.
     */
    fun screenReplyOrNull(reply: String): String? {
        val text = reply.trim()
        if (text.isEmpty()) return null
        for (category in categories) {
            if (category.patterns.any { it.containsMatchIn(text) }) {
                return "Let's talk about something else — how about a game or your favorite hobby?"
            }
        }
        return null
    }
}
