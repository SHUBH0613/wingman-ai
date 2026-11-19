package com.wingman.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DatingReplyBundle(
    val detectedStage: String,
    val options: List<String>,
    val strategicTip: String
)

object GeminiDatingEngine {

    const val PRIMARY_MODEL = "gemini-2.0-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.substring(7).trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3).trim()
        }
        return cleaned
    }

    suspend fun generateReplies(
        apiKey: String,
        contextText: String,
        stage: DatingStage,
        profile: FullUserProfile,
        language: LanguageMode,
        flavor: HumorFlavor
    ): DatingReplyBundle = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext DatingReplyBundle(
                detectedStage = "ERROR",
                options = listOf("Please add your Gemini API Key in Wingman Settings first!"),
                strategicTip = "API key missing."
            )
        }

        val languageInstructions = when (language) {
            LanguageMode.ENGLISH -> "Respond in natural, witty, modern English."
            LanguageMode.HINGLISH -> """
                Respond in authentic HINGLISH (Hindi-English mix commonly texted by young professionals in Mumbai, Delhi, Bangalore).
                Use casual romanized Hindi ('scene kya hai', 'batao', 'sahi hai', 'drama', 'chai/coffee pe chalte hain').
                Avoid overly formal or cringe Bollywood lines; keep it charming, urban, and modern.
            """.trimIndent()
            LanguageMode.BILINGUAL_MIX -> "Provide a mix: Option 1 in English, Option 2 in Hinglish, Option 3 in a smooth bilingual blend."
        }

        val flavorInstructions = when (flavor) {
            HumorFlavor.WITTY_TEASE -> "High playful push-pull, light teasing, self-assured confidence."
            HumorFlavor.FILMY_DESI -> "Subtle, witty pop-culture or cinema references (Imtiaz Ali, Bollywood music, iconic relatable debates)."
            HumorFlavor.DRY_SARCASM -> "Deadpan, observant, clever sarcasm, zero exclamation marks."
            HumorFlavor.DATE_CLOSER -> "Smooth, casual pivot to meeting for coffee, drinks, or matcha this Thursday or Friday."
        }

        val systemPrompt = """
            You are Wingman AI, an elite dating conversational strategist for Bumble, Hinge, and Instagram DMs.
            You have studied top-performing male profiles, viral Instagram hook formats, and high-conversion dating psychology.
            
            USER CONTEXT:
            - Name: ${profile.vitals.name}
            - City / Favorite Spots: ${profile.vitals.city.ifBlank { "Local coffee shops or speakeasies" }}
            - Career / College: ${profile.vitals.work} / ${profile.vitals.college}
            - Hobbies / Passions: ${profile.interests.hobbies.joinToString(", ")}
            - Dating Goals: ${profile.lifestyle.datingIntention}
            
            LANGUAGE & STYLE:
            $languageInstructions
            $flavorInstructions
            
            REQUESTED STAGE: ${stage.name}
            
            CRITICAL RULES FOR DATING CHAT:
            1. Zero generic AI phrases ('That sounds fascinating!', 'I completely understand', etc.).
            2. Match texting etiquette: 1 to 2 lines max, casual punctuation, natural flow.
            3. Never be lame or overly eager. Maintain equal or higher status with warmth.
            4. If OPENER: focus on a specific, non-obvious detail from her bio or photo.
            5. If DATE CLOSER: propose a concrete low-pressure activity in ${profile.vitals.city.ifBlank { "the city" }} this week.
            
            Return ONLY a valid JSON object matching this schema:
            {
              "detectedStage": "Banter & Chemistry",
              "options": [
                "option 1",
                "option 2",
                "option 3"
              ],
              "strategicTip": "Keep the tease alive; let her qualify herself."
            }
        """.trimIndent()

        val prompt = "Match Profile / Chat Context:\n\"$contextText\"\n\nGenerate the 3 options and strategic tip."

        val requestJson = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            val partsArray = com.google.gson.JsonArray()

            partsArray.add(JsonObject().apply {
                addProperty("text", "$systemPrompt\n\n$prompt")
            })

            contentObj.add("parts", partsArray)
            contentsArray.add(contentObj)
            add("contents", contentsArray)

            val genConfig = JsonObject().apply {
                addProperty("responseMimeType", "application/json")
                addProperty("temperature", 0.85)
            }
            add("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$PRIMARY_MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext DatingReplyBundle(
                    detectedStage = "ERROR",
                    options = listOf("Error calling Gemini: HTTP ${response.code}"),
                    strategicTip = "Check network or API key."
                )
            }

            val responseObj = gson.fromJson(body, JsonObject::class.java)
            val candidates = responseObj?.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    val rawText = parts[0].asJsonObject.get("text")?.asString?.trim() ?: ""
                    val cleaned = cleanJson(rawText)
                    val bundle = gson.fromJson(cleaned, DatingReplyBundle::class.java)
                    if (bundle != null && bundle.options.isNotEmpty()) {
                        return@withContext bundle
                    }
                }
            }
            DatingReplyBundle("UNKNOWN", listOf("Could not parse response."), "")
        } catch (e: Exception) {
            DatingReplyBundle("ERROR", listOf("Failed to connect: ${e.localizedMessage ?: "Unknown"}"), "")
        }
    }
}
