package com.wingman.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WingmanUnitTest {

    @Test
    fun testDatingStageEnumValues() {
        val stages = DatingStage.values()
        assertEquals(5, stages.size)
        assertTrue(stages.contains(DatingStage.AUTO_DETECT))
        assertTrue(stages.contains(DatingStage.STAGE_1_HOOK_OPENER))
        assertTrue(stages.contains(DatingStage.STAGE_2_BANTER_TEASE))
        assertTrue(stages.contains(DatingStage.STAGE_3_COMMON_GROUND))
        assertTrue(stages.contains(DatingStage.STAGE_4_DATE_CLOSER))
    }

    @Test
    fun testLanguageModeAndHumorFlavor() {
        assertEquals(3, LanguageMode.values().size)
        assertEquals(4, HumorFlavor.values().size)
    }

    @Test
    fun testViralHookLibraryFiltering() {
        val allHooks = ViralHookLibrary.curatedHooks
        assertTrue(allHooks.isNotEmpty())

        val englishHooks = ViralHookLibrary.getHooksFor(LanguageMode.ENGLISH)
        assertTrue(englishHooks.isNotEmpty())
        assertTrue(englishHooks.all { it.language == LanguageMode.ENGLISH })

        val hinglishHooks = ViralHookLibrary.getHooksFor(LanguageMode.HINGLISH)
        assertTrue(hinglishHooks.isNotEmpty())

        val bilingualHooks = ViralHookLibrary.getHooksFor(LanguageMode.BILINGUAL_MIX)
        assertEquals(allHooks.size, bilingualHooks.size)
    }

    @Test
    fun testFullUserProfileConstruction() {
        val profile = FullUserProfile(
            vitals = UserVitals(name = "Alex", age = 27, city = "Bandra"),
            lifestyle = UserLifestyle(datingIntention = "Relationship"),
            interests = UserInterests(hobbies = listOf("F1", "Guitar")),
            preferredLanguage = LanguageMode.HINGLISH,
            preferredHumor = HumorFlavor.WITTY_TEASE
        )
        assertNotNull(profile)
        assertEquals("Alex", profile.vitals.name)
        assertEquals(27, profile.vitals.age)
        assertEquals("Bandra", profile.vitals.city)
        assertEquals("Relationship", profile.lifestyle.datingIntention)
        assertEquals(2, profile.interests.hobbies.size)
        assertEquals(LanguageMode.HINGLISH, profile.preferredLanguage)
        assertEquals(HumorFlavor.WITTY_TEASE, profile.preferredHumor)
    }
}
