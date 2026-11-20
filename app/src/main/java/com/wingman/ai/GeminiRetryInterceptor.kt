package com.wingman.ai

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.random.Random

/**
 * OkHttp Interceptor that retries temporary Gemini API failures
 * (HTTP 429 Too Many Requests / Resource Exhausted, and HTTP 503 Service Unavailable)
 * using exponential backoff with randomized jitter.
 */
class GeminiRetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var attempt = 0
        var currentDelay = initialDelayMs

        while (attempt <= maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.code != 429 && response.code != 503) {
                    return response
                }
            } catch (e: IOException) {
                if (attempt >= maxRetries) throw e
            }

            attempt++
            if (attempt > maxRetries) break

            val jitter = Random.nextInt(0, 250)
            try {
                Thread.sleep(currentDelay + jitter)
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            currentDelay *= 2
        }

        return response ?: chain.proceed(request)
    }
}
