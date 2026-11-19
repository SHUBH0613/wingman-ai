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

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateReplies(
        apiKey: String,
        contextText: String,
        stage: DatingStage,
        userCityAndSpots: String,
        userToneStyle: String
    ): DatingReplyBundle = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext DatingReplyBundle(
                detectedStage = "ERROR",
                options = listOf("Please add your Gemini API Key in Wingman Settings first!"),
                strategicTip = "API key missing."
            )
        }

        val systemPrompt = """
            You are Wingman AI, an elite dating conversational strategist for Bumble, Hinge, and Instagram.
            You follow the proven 4-Stage Date Closer Playbook:
            
            THE 4-STAGE PLAYBOOK:
            - STAGE 1: HOOK OPENER (Observation on a prompt/photo, playful challenge, zero generic 'Hey')
            - STAGE 2: BANTER & TEASE (Playful push-pull, witty teasing, matching response length)
            - STAGE 3: COMMON GROUND (Bridge to mutual interests, food, drinks, or weekend plans)
            - STAGE 4: DATE CLOSER (Low-pressure invitation to drinks/coffee at a specific spot on Thursday/Friday, plus contact bridge)
            
            USER CONTEXT:
            - City / Favorite Hangouts: ${userCityAndSpots.ifBlank { "Local neighborhood cafe or speakeasy cocktails" }}
            - Humor / Persona: ${userToneStyle.ifBlank { "Playfully confident, observant, dry wit, unbothered" }}
            - Requested Stage: ${stage.name}
            
            RULES FOR HUMAN-LIKE TEXTS:
            1. NEVER sound like ChatGPT. No exclamation mark overload, no formal essays.
            2. Match modern texting etiquette: casual syntax, occasional lowercase, concise (1-2 sentences).
            3. Always give 3 distinct options with varying levels of boldness:
               - Option A: Balanced & Charming
               - Option B: Bold / Flirty Tease
               - Option C: Direct Date / Pivot Closer
            
            Return ONLY a valid JSON object matching:
            {
              "detectedStage": "Stage 2: Banter",
              "options": [
                "option 1",
                "option 2",
                "option 3"
              ],
              "strategicTip": "She's matching your energy—pivot to drinks in your next text."
            }
        """.trimIndent()

        val prompt = "Current Match Profile / Chat Context:\n\"$contextText\"\n\nGenerate the 3 options and strategic tip."

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
                addProperty("temperature", 0.8)
            }
            add("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
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
                    options = listOf("Error calling Gemini API: HTTP ${response.code}"),
                    strategicTip = "Check network or API key."
                )
            }

            val responseObj = gson.fromJson(body, JsonObject::class.java)
            val candidates = responseObj.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val text = candidate.getAsJsonObject("content").getAsJsonArray("parts")[0].asJsonObject.get("text").asString.trim()
                return@withContext gson.fromJson(text, DatingReplyBundle::class.java)
            }
            DatingReplyBundle("UNKNOWN", listOf("Could not parse response."), "")
        } catch (e: Exception) {
            DatingReplyBundle("ERROR", listOf("Failed to connect: ${e.localizedMessage ?: "Unknown"}"), "")
        }
    }
}
