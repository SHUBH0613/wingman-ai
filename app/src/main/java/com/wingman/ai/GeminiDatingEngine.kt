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

enum class DatingStage {
    AUTO_DETECT,
    STAGE_1_HOOK_OPENER,
    STAGE_2_BANTER_TEASE,
    STAGE_3_COMMON_GROUND,
    STAGE_4_DATE_CLOSER
}

data class DatingReplyBundle(
    val detectedStage: String,
    val options: List<String>,
    val strategicTip: String
)

object GeminiDatingEngine {

    const val PRIMARY_MODEL = "gemini-3.5-flash-lite"

    private val client = OkHttpClient.Builder()
        .addInterceptor(GeminiRetryInterceptor())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun parseGeminiError(statusCode: Int, body: String): String {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val errObj = json?.getAsJsonObject("error")
            if (errObj != null) {
                val msg = errObj.get("message")?.asString ?: "Unknown API error"
                val status = errObj.get("status")?.asString ?: "HTTP $statusCode"
                "API Error ($status): $msg"
            } else {
                "HTTP $statusCode: ${body.take(120)}"
            }
        } catch (e: Exception) {
            "HTTP $statusCode error calling Gemini API"
        }
    }

    fun cleanJson(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstLineBreak = text.indexOf('\n')
            if (firstLineBreak != -1) {
                text = text.substring(firstLineBreak + 1)
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3)
            }
        }
        return text.trim()
    }

    /**
     * Sanitizes extracted screen text to protect against adversarial prompt injection
     */
    fun sanitizeInputContext(rawContext: String): String {
        val dangerousPatterns = listOf(
            Regex("(?i)ignore (all )?previous instructions"),
            Regex("(?i)system prompt"),
            Regex("(?i)you are now a"),
            Regex("(?i)reveal secret"),
            Regex("(?i)forget your rules")
        )
        var sanitized = rawContext
        for (pattern in dangerousPatterns) {
            sanitized = sanitized.replace(pattern, "[redacted]")
        }
        return sanitized.take(1500)
    }

    suspend fun generateReplies(
        apiKey: String,
        contextText: String,
        stage: DatingStage,
        profile: FullUserProfile,
        language: LanguageMode,
        flavor: HumorFlavor
    ): DatingReplyBundle = withContext(Dispatchers.IO) {
        val effectiveKey = apiKey.ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }.trim()
        if (effectiveKey.isBlank()) {
            return@withContext DatingReplyBundle(
                detectedStage = "ERROR",
                options = listOf("Please enter or verify your Gemini API Key in Settings!"),
                strategicTip = "Missing API key."
            )
        }

        val cleanContext = sanitizeInputContext(contextText)

        val languageInstructions = when (language) {
            LanguageMode.ENGLISH -> "Respond in natural, witty, modern English."
            LanguageMode.HINGLISH -> """
                Respond in authentic HINGLISH (natural Hindi-English mix as texted by urban 20-somethings in Mumbai, Delhi, Bangalore).
                Use casual romanized words like 'scene kya hai', 'batao', 'sahi hai', 'drama', 'chai/coffee pe chalte hain'.
                Avoid cringe or overly formal Bollywood lines; keep it charming, urban, and effortless.
            """.trimIndent()
            LanguageMode.BILINGUAL_MIX -> "Option 1 in English, Option 2 in Hinglish, Option 3 in a smooth bilingual blend."
        }

        val flavorInstructions = when (flavor) {
            HumorFlavor.WITTY_TEASE -> "High-status playful push-pull, light teasing, teasing challenges, self-assured confidence."
            HumorFlavor.FILMY_DESI -> "Pop-culture wit, cinema references (Imtiaz Ali vs Anurag Kashyap, indie music, relatable debates)."
            HumorFlavor.DRY_SARCASM -> "Deadpan, observant, clever sarcasm, zero exclamation marks."
            HumorFlavor.DATE_CLOSER -> "Smooth, casual pivot to meeting for coffee, drinks, or matcha this Thursday or Friday."
        }

        val systemPrompt = """
            You are Wingman AI, an elite dating conversational strategist specializing in "The High-Status Charmer" archetype.
            You have studied top-performing male profiles, viral Instagram hook formats, and high-conversion dating psychology.
            
            USER PROFILE CONTEXT:
            - Name: ${profile.vitals.name}
            - City / Favorite Spots: ${profile.vitals.city.ifBlank { "Local coffee spots or rooftop bars" }}
            - Career / Education: ${profile.vitals.work} / ${profile.vitals.college}
            - Hobbies & Quirks: ${profile.interests.hobbies.joinToString(", ")}
            - Dating Intention: ${profile.lifestyle.datingIntention}
            
            LANGUAGE & TONE:
            $languageInstructions
            $flavorInstructions
            
            STAGE GOAL: ${stage.name}
            
            THE HIGH-STATUS CHARMER RULES:
            1. NEVER sound like a generic AI or eager text bot. No 'That sounds fascinating!', 'Oh wow!', or polite essays.
            2. Match texting etiquette: 1 to 2 lines max, casual capitalization, conversational punctuation.
            3. Maintain high status: You are playfully confident, observant, slightly challenging, and never seeking approval.
            4. If OPENER: focus on a specific, non-obvious detail from her bio or photo. Never use 'Hey' or compliment appearance directly.
            5. If BANTER: use subtle push-pull ('We would either be best friends or rivals, no in-between').
            6. If DATE CLOSER: propose a concrete low-pressure activity in ${profile.vitals.city.ifBlank { "the city" }} on Thursday or Friday.
            
            Return ONLY a valid JSON object matching:
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

        val prompt = "Conversation History / Match Profile Context:\n\"$cleanContext\"\n\nGenerate the 3 options and strategic tip."

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

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$PRIMARY_MODEL:generateContent?key=$effectiveKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorDetail = parseGeminiError(response.code, body)
                val tip = when (response.code) {
                    400 -> "Verify your Gemini API key format and parameters."
                    403 -> "API key lacks permission or quota exhausted."
                    404 -> "Model endpoint not found. Using $PRIMARY_MODEL."
                    429 -> "Rate limit exhausted after retries. Please wait a moment."
                    503 -> "Google Gemini service temporarily overloaded."
                    else -> "Check internet connection or verify API key in Settings."
                }
                return@withContext DatingReplyBundle(
                    detectedStage = "ERROR",
                    options = listOf(errorDetail),
                    strategicTip = tip
                )
            }

            val responseObj = gson.fromJson(body, JsonObject::class.java)
            val candidates = responseObj.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    val rawText = parts[0].asJsonObject.get("text")?.asString ?: "{}"
                    val clean = cleanJson(rawText)
                    return@withContext gson.fromJson(clean, DatingReplyBundle::class.java)
                }
            }
            DatingReplyBundle("UNKNOWN", listOf("Could not parse response from Gemini."), "")
        } catch (e: Exception) {
            DatingReplyBundle("ERROR", listOf("Failed to connect: ${e.localizedMessage ?: "Unknown"}"), "")
        }
    }
}
