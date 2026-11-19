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

data class HingePrompt(val promptTitle: String, val answer: String)
data class ProfileResult(
    val hingePacks: List<List<HingePrompt>>,
    val bumbleBios: List<String>,
    val openingMoves: List<String>,
    val hinglishBonusBio: String
)

object ProfileGenerator {

    const val PRIMARY_MODEL = "gemini-2.0-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
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

    suspend fun generateProfilePacks(
        apiKey: String,
        profile: FullUserProfile
    ): ProfileResult? = withContext(Dispatchers.IO) {
        val effectiveKey = apiKey.ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }.trim()
        if (effectiveKey.isBlank()) return@withContext null

        val systemPrompt = """
            You are the world's leading dating profile consultant specializing in male profiles for Hinge and Bumble.
            You have studied top 1% dating profiles, viral Instagram reels, and modern psychological attraction triggers.
            
            USER PROFILE DETAILS:
            - Name: ${profile.vitals.name}, Age: ${profile.vitals.age}, Height: ${profile.vitals.height}
            - City / Hometown: ${profile.vitals.city}
            - Career & College: ${profile.vitals.work} / ${profile.vitals.college}
            - Lifestyle Badges: Intention: ${profile.lifestyle.datingIntention}, Workout: ${profile.lifestyle.workout}, Drinking: ${profile.lifestyle.drinking}, Smoking: ${profile.lifestyle.smoking}
            - Hobbies & Skills: ${profile.interests.hobbies.joinToString(", ")}; Skills: ${profile.interests.skills.joinToString(", ")}
            - Music / Food / Quirks: ${profile.interests.musicAndFood}
            - Pet Peeve / Guilty Pleasure: ${profile.interests.petPeeve} / ${profile.interests.guiltyPleasure}
            
            RULES:
            1. No clichés ('fluent in sarcasm', 'loves travel, dogs and pizza', 'partner in crime').
            2. High Conversation Bait: Give matches effortless reasons to message first.
            3. Balance: 1 part ambition/status, 1 part self-deprecating wit, 1 part genuine warmth.
            4. Include both sharp modern English bios and 1 witty Hinglish bonus bio for urban Indian dating appeal.
            
            Return ONLY a valid JSON object matching:
            {
              "hingePacks": [
                [
                  {"promptTitle": "The way to win me over is", "answer": "..."},
                  {"promptTitle": "Together, we could", "answer": "..."},
                  {"promptTitle": "Two truths and a lie", "answer": "..."}
                ],
                [
                  {"promptTitle": "I'm convinced that", "answer": "..."},
                  {"promptTitle": "A quick rant about", "answer": "..."},
                  {"promptTitle": "Dating me is like", "answer": "..."}
                ],
                [
                  {"promptTitle": "My simple pleasures", "answer": "..."},
                  {"promptTitle": "I'll brag about you to my friends if", "answer": "..."},
                  {"promptTitle": "Best travel story", "answer": "..."}
                ]
              ],
              "bumbleBios": [
                "Bumble bio 1 under 280 chars",
                "Bumble bio 2 under 280 chars",
                "Bumble bio 3 under 280 chars"
              ],
              "openingMoves": [
                "Opening move question 1",
                "Opening move question 2"
              ],
              "hinglishBonusBio": "Urban witty Hinglish bio under 280 chars"
            }
        """.trimIndent()

        val requestJson = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            val partsArray = com.google.gson.JsonArray()

            partsArray.add(JsonObject().apply {
                addProperty("text", systemPrompt)
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
                    return@withContext gson.fromJson(cleaned, ProfileResult::class.java)
                }
            }
            null
        } catch (e: Throwable) {
            null
        }
    }
}
