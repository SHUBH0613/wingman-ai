package com.wingman.ai

import android.content.SharedPreferences

enum class LanguageMode {
    ENGLISH,
    HINGLISH,
    BILINGUAL_MIX
}

enum class HumorFlavor {
    WITTY_TEASE,
    FILMY_DESI,
    DRY_SARCASM,
    DATE_CLOSER
}

data class UserVitals(
    val name: String = "",
    val age: Int = 0,
    val gender: String = "Male",
    val height: String = "",
    val city: String = "",
    val work: String = "",
    val college: String = ""
)

data class UserLifestyle(
    val datingIntention: String = "Long-term, open to short",
    val workout: String = "Active / Gym",
    val drinking: String = "Socially",
    val smoking: String = "Never",
    val religion: String = "",
    val politics: String = ""
)

data class UserInterests(
    val hobbies: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val musicAndFood: String = "",
    val petPeeve: String = "",
    val guiltyPleasure: String = ""
)

data class FullUserProfile(
    val vitals: UserVitals,
    val lifestyle: UserLifestyle,
    val interests: UserInterests,
    val preferredLanguage: LanguageMode = LanguageMode.ENGLISH,
    val preferredHumor: HumorFlavor = HumorFlavor.WITTY_TEASE
) {
    companion object {
        // Safely injected from BuildConfig at build time (e.g. from GitHub Secret or local.properties), never hardcoded in git
        val BUNDLED_DEFAULT_API_KEY: String
            get() = BuildConfig.DEFAULT_GEMINI_API_KEY

        fun fromPreferences(prefs: SharedPreferences): FullUserProfile {
            val city = prefs.getString("user_city", null) ?: prefs.getString("user_spots", "") ?: ""
            val hobbiesRaw = prefs.getString("user_hobbies", null) ?: prefs.getString("user_passions", "") ?: ""
            val hobbiesList = hobbiesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            return FullUserProfile(
                vitals = UserVitals(
                    name = prefs.getString("user_name", "") ?: "",
                    age = prefs.getString("user_age", "25")?.toIntOrNull() ?: 25,
                    height = prefs.getString("user_height", "5'11") ?: "5'11",
                    city = city,
                    work = prefs.getString("user_work", "") ?: "",
                    college = prefs.getString("user_college", "") ?: ""
                ),
                lifestyle = UserLifestyle(
                    datingIntention = prefs.getString("user_intent", "Connection") ?: "Connection",
                    workout = prefs.getString("user_habits", "Active") ?: "Active"
                ),
                interests = UserInterests(
                    hobbies = hobbiesList,
                    musicAndFood = prefs.getString("user_music_food", "") ?: ""
                )
            )
        }
    }
}
