package com.wingman.ai

enum class DatingStage {
    AUTO_DETECT,
    STAGE_1_HOOK_OPENER,
    STAGE_2_BANTER_TEASE,
    STAGE_3_COMMON_GROUND,
    STAGE_4_DATE_CLOSER
}

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
    val datingIntention: String = "Long-term, open to short", // e.g. "Looking for a relationship", "Figuring it out"
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
)
