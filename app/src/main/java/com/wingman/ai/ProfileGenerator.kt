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

data class ProfileQuizData(
    val passions: String,
    val weekendVibe: String,
    val guiltyPleasure: String,
    val petPeeve: String,
    val humorStyle: String,
    val city: String
)

data class HingePrompt(val promptTitle: String, val answer: String)
data class ProfileResult(
    val hingePacks: List<List<HingePrompt>>,
    val bumbleBios: List<String>,
    val openingMoves: List<String>
)

object ProfileGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateProfilePacks(
        apiKey: String,
        quiz: ProfileQuizData
    ): ProfileResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val systemPrompt = """
            You are the world's top dating profile consultant for Hinge and Bumble.
            You craft witty, authentic, attractive profiles that generate high match rates and high conversation starters.
            
            RULES FOR DATING PROFILES:
            1. No clichés ('I love travel, gym, and good food', 'Partner in crime').
            2. High Conversation Bait: Every prompt/bio must give matches an effortless reason to message first.
            3. Humor & Edge: 1 part self-deprecating/playful wit, 1 part genuine personality.
            4. Keep Bumble bios under 280 characters.
            
            Return ONLY a valid JSON object matching this schema:
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
                  {"promptTitle": "Best travel story", "answer": "..."},
                  {"promptTitle": "My simple pleasures", "answer": "..."},
                  {"promptTitle": "I'll brag about you to my friends if", "answer": "..."}
                ]
              ],
              "bumbleBios": [
                "Bumble bio option 1 with hook (under 280 chars)",
                "Bumble bio option 2 with hook (under 280 chars)",
                "Bumble bio option 3 with hook (under 280 chars)"
              ],
              "openingMoves": [
                "Bumble opening move question 1",
                "Bumble opening move question 2",
                "Bumble opening move question 3"
              ]
            }
        """.trimIndent()

        val userPrompt = """
            User Quiz Profile:
            - City: ${quiz.city}
            - Passions/Obsessions: ${quiz.passions}
            - Weekend Routine: ${quiz.weekendVibe}
            - Guilty Pleasure: ${quiz.guiltyPleasure}
            - Pet Peeve: ${quiz.petPeeve}
            - Humor Style: ${quiz.humorStyle}
            
            Generate the 3 complete Hinge packs, 3 Bumble bios, and 3 opening moves.
        """.trimIndent()

        val requestJson = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            val partsArray = com.google.gson.JsonArray()

            partsArray.add(JsonObject().apply {
                addProperty("text", "$systemPrompt\n\n$userPrompt")
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

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext null

            val responseObj = gson.fromJson(body, JsonObject::class.java)
            val candidates = responseObj.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val text = candidate.getAsJsonObject("content").getAsJsonArray("parts")[0].asJsonObject.get("text").asString
                return@withContext gson.fromJson(text, ProfileResult::class.java)
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
