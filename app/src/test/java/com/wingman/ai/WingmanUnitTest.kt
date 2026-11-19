package com.wingman.ai

import com.google.gson.Gson
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

    @Test
    fun testCleanJsonWithMarkdownFences() {
        val rawMarkdown = """
            ```json
            {
              "detectedStage": "Banter",
              "options": ["Hey", "Hi"],
              "strategicTip": "Keep teasing"
            }
            ```
        """.trimIndent()

        val cleaned = GeminiDatingEngine.cleanJson(rawMarkdown)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))

        val bundle = Gson().fromJson(cleaned, DatingReplyBundle::class.java)
        assertEquals("Banter", bundle.detectedStage)
        assertEquals(2, bundle.options.size)
        assertEquals("Keep teasing", bundle.strategicTip)
    }

    @Test
    fun testCleanJsonPlain() {
        val plainJson = """{"photoScores":[],"optimalSequence":[0,1],"overallSummary":"Good"}"""
        val cleaned = PhotoAuditor.cleanJson(plainJson)
        assertEquals(plainJson, cleaned)

        val report = Gson().fromJson(cleaned, PhotoAuditReport::class.java)
        assertEquals(2, report.optimalSequence.size)
        assertEquals("Good", report.overallSummary)
    }

    @Test
    fun testProfileResultParsing() {
        val sampleJson = """
            ```json
            {
              "hingePacks": [
                [{"promptTitle": "Win me over", "answer": "Good tacos"}]
              ],
              "bumbleBios": ["Bio 1"],
              "openingMoves": ["Move 1"],
              "hinglishBonusBio": "Chai and talks"
            }
            ```
        """.trimIndent()
        val cleaned = ProfileGenerator.cleanJson(sampleJson)
        val result = Gson().fromJson(cleaned, ProfileResult::class.java)
        assertNotNull(result)
        assertEquals(1, result.hingePacks.size)
        assertEquals(1, result.bumbleBios.size)
        assertEquals("Bio 1", result.bumbleBios[0])
        assertEquals("Chai and talks", result.hinglishBonusBio)
    }

    @Test
    fun testGeminiModelConstant() {
        assertEquals("gemini-2.0-flash", GeminiDatingEngine.PRIMARY_MODEL)
        assertEquals("gemini-2.0-flash", PhotoAuditor.PRIMARY_MODEL)
        assertEquals("gemini-2.0-flash", ProfileGenerator.PRIMARY_MODEL)
    }
}
