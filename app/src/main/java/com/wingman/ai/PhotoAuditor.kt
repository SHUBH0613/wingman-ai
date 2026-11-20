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

data class PhotoMetricScore(
    val photoIndex: Int,
    val overallScore: Double, // 1 to 10
    val lightingScore: Int,
    val smileAndEyeContact: Int,
    val lifestyleSignals: Int,
    val redFlags: List<String>,
    val recommendation: String
)

data class PhotoAuditReport(
    val photoScores: List<PhotoMetricScore>,
    val optimalSequence: List<Int>, // ordered photo indices e.g. [2, 0, 4, 1, 5, 3]
    val overallSummary: String
)

object PhotoAuditor {

    const val PRIMARY_MODEL = "gemini-3.5-flash-lite"

    private val client = OkHttpClient.Builder()
        .addInterceptor(GeminiRetryInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    suspend fun auditPhotos(
        apiKey: String,
        base64Images: List<String>
    ): PhotoAuditReport? = withContext(Dispatchers.IO) {
        val effectiveKey = apiKey.ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }.trim()
        if (effectiveKey.isBlank() || base64Images.isEmpty()) return@withContext null

        val systemPrompt = """
            You are an elite dating photo consultant with deep expertise in dating app psychology (Hinge, Tinder, Bumble).
            You analyze a set of user photos and provide an objective 6-metric audit and rank the best 6-photo sequence.
            
            SCORING METRICS:
            1. First Impression & Attractiveness (1-10)
            2. Lighting & Clarity (1-10)
            3. Eye Contact & Warmth/Smile (1-10)
            4. Lifestyle / Hobby / Social proof signals (1-10)
            5. Red Flag Detection (e.g., sunglasses, gym selfies, group confusion, hats covering face, bad crops)
            6. Role in Sequence: Slot 1 (Headshot), Slot 2 (Full Body), Slot 3 (Activity/Hobby), Slot 4 (Social), Slot 5 (Travel/Candid), Slot 6 (Personality/Humor).
            
            Return ONLY a valid JSON object matching this schema:
            {
              "photoScores": [
                {
                  "photoIndex": 0,
                  "overallScore": 8.5,
                  "lightingScore": 9,
                  "smileAndEyeContact": 8,
                  "lifestyleSignals": 7,
                  "redFlags": ["Slightly dark background"],
                  "recommendation": "Best suited for Primary Headshot (Slot 1)"
                }
              ],
              "optimalSequence": [0, 2, 1, 3],
              "overallSummary": "Your lineup is strong on lifestyle, but replace Photo #2 with a brighter solo picture."
            }
        """.trimIndent()

        val contentsArray = com.google.gson.JsonArray()
        val contentObj = JsonObject()
        val partsArray = com.google.gson.JsonArray()

        partsArray.add(JsonObject().apply {
            addProperty("text", "$systemPrompt\n\nAnalyze these ${base64Images.size} photos and provide the audit report.")
        })

        // Add each image as inlineData
        for (b64 in base64Images) {
            val inlineData = JsonObject().apply {
                addProperty("mimeType", "image/jpeg")
                addProperty("data", b64)
            }
            partsArray.add(JsonObject().apply {
                add("inlineData", inlineData)
            })
        }

        contentObj.add("parts", partsArray)
        contentsArray.add(contentObj)

        val requestJson = JsonObject().apply {
            add("contents", contentsArray)
            val genConfig = JsonObject().apply {
                addProperty("responseMimeType", "application/json")
                addProperty("temperature", 0.4)
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
            if (!response.isSuccessful) return@withContext null

            val responseObj = gson.fromJson(body, JsonObject::class.java)
            val candidates = responseObj?.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    val rawText = parts[0].asJsonObject.get("text")?.asString ?: ""
                    val cleaned = cleanJson(rawText)
                    return@withContext gson.fromJson(cleaned, PhotoAuditReport::class.java)
                }
            }
            null
        } catch (e: Throwable) {
            null
        }
    }
}
