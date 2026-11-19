package com.wingman.ai

data class ViralHook(
    val category: String,
    val hookText: String,
    val language: LanguageMode,
    val strategyNote: String
)

object ViralHookLibrary {

    val curatedHooks = listOf(
        // Observational / Curiosity Hooks (English)
        ViralHook(
            category = "Curiosity Hook",
            hookText = "Tell me the story behind photo #3 before I make up a completely false narrative about it.",
            language = LanguageMode.ENGLISH,
            strategyNote = "Triggers an open-ended explanation without being judgmental or generic."
        ),
        ViralHook(
            category = "Playful Tease",
            hookText = "We are either going to get along ridiculously well or debate constantly. No middle ground.",
            language = LanguageMode.ENGLISH,
            strategyNote = "High status playfully confident challenge; invites her to prove herself."
        ),
        ViralHook(
            category = "Food / Vibe Debate",
            hookText = "Your profile says you love sushi, but what's your controversial food opinion? Don't hold back.",
            language = LanguageMode.ENGLISH,
            strategyNote = "Effortless conversation starter with low cognitive barrier."
        ),

        // Hinglish & Desi Cultural Wit Hooks
        ViralHook(
            category = "Hinglish Banter",
            hookText = "Apna coffee order batao pehle, taaki mujhe pata chale kitna drama expect karna hai.",
            language = LanguageMode.HINGLISH,
            strategyNote = "Light-hearted, relatable urban banter used by top Delhi/Mumbai profiles."
        ),
        ViralHook(
            category = "Desi Pop-Culture",
            hookText = "Are you more of an Imtiaz Ali romantic or an Anurag Kashyap chaotic person? Choose wisely.",
            language = LanguageMode.HINGLISH,
            strategyNote = "Instant pop-culture vibe check that consistently yields long replies."
        ),
        ViralHook(
            category = "Hinglish Travel Hook",
            hookText = "Pahaad ya beach? Agar beach bola toh weekend trip ki planning cancel samjho.",
            language = LanguageMode.HINGLISH,
            strategyNote = "Playful push-pull with high reply rates on Bumble Opening Moves."
        ),

        // Date Closer Hooks (Direct & Low Pressure)
        ViralHook(
            category = "Date Closer",
            hookText = "We could exchange 40 more texts here, or grab coffee/cocktails this Thursday and see if the banter translates.",
            language = LanguageMode.ENGLISH,
            strategyNote = "Moves off the app smoothly once mutual chemistry is established."
        ),
        ViralHook(
            category = "Hinglish Date Closer",
            hookText = "App pe text karna boring ho raha hai. Tell me your favorite chai or coffee spot, let's test this chemistry live.",
            language = LanguageMode.HINGLISH,
            strategyNote = "Casual and low pressure transition to real life."
        )
    )

    fun getHooksFor(language: LanguageMode): List<ViralHook> {
        return if (language == LanguageMode.BILINGUAL_MIX) {
            curatedHooks
        } else {
            curatedHooks.filter { it.language == language || it.language == LanguageMode.ENGLISH }
        }
    }
}
