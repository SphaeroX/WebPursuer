package com.example.webpursuer.network

import com.example.webpursuer.data.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenRouterService(
        private val settingsRepository: SettingsRepository,
        private val logRepository: com.example.webpursuer.data.LogRepository? = null
) {
    // ... (Interceptor logic omitted for brevity, keeping existing)

    suspend fun checkContent(
            prompt: String,
            content: String,
            useWebSearch: Boolean = false
    ): Boolean {
        logRepository?.logInfo(
                "LLM",
                "checkContent called request for prompt: ${prompt.take(50)}..."
        )
        val apiKey = settingsRepository.apiKey.first() ?: return false
        val model = settingsRepository.monitorModel.first() // Use Monitor Model

        val client =
                OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request =
                                    chain.request()
                                            .newBuilder()
                                            .addHeader("Authorization", "Bearer $apiKey")
                                            .addHeader(
                                                    "HTTP-Referer",
                                                    "https://github.com/example/webpursuer"
                                            )
                                            .addHeader("X-Title", "WebPursuer")
                                            .build()
                            chain.proceed(request)
                        }
                        .build()
        // ... (rest of client setup same as before, likely could refactor but keeping changes
        // minimal for now)
        val retrofit =
                Retrofit.Builder()
                        .baseUrl("https://openrouter.ai/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        val systemPrompt =
                "You are a helpful assistant that analyzes web content. You will be given a user prompt and the content of a webpage. You must decide if the user's condition is met. Reply ONLY with 'YES' or 'NO'."
        val userMessage = "User Condition: $prompt\n\nWeb Content:\n$content"

        try {
            val response =
                    api.getCompletion(
                            ChatRequest(
                                    model = model,
                                    messages =
                                            listOf(
                                                    Message("system", systemPrompt),
                                                    Message("user", userMessage)
                                            ),
                                    plugins = if (useWebSearch) listOf(Plugin("web")) else null
                            )
                    )

            val reply = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase()
            logRepository?.logInfo("LLM", "checkContent response: $reply")
            return reply != null &&
                    (reply == "YES" || reply.contains("YES")) // Relaxed check slightly
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository?.logError(
                    "LLM",
                    "checkContent failed: ${e.message}",
                    e.stackTraceToString()
            )
            return false
        }
    }

    suspend fun interpretContent(
            instruction: String,
            content: String,
            useWebSearch: Boolean = false
    ): String {
        logRepository?.logInfo(
                "LLM",
                "interpretContent called with instruction: ${instruction.take(50)}..."
        )
        val apiKey =
                settingsRepository.apiKey.first()
                        ?: return content // Fallback to original if no key
        val model = settingsRepository.monitorModel.first()

        val client =
                OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request =
                                    chain.request()
                                            .newBuilder()
                                            .addHeader("Authorization", "Bearer $apiKey")
                                            .addHeader(
                                                    "HTTP-Referer",
                                                    "https://github.com/example/webpursuer"
                                            )
                                            .addHeader("X-Title", "WebPursuer")
                                            .build()
                            chain.proceed(request)
                        }
                        .build()
        val retrofit =
                Retrofit.Builder()
                        .baseUrl("https://openrouter.ai/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        val systemPrompt =
                "You are an AI interpreter for a web monitor. Your task is to process the raw webpage content according to the user's instruction (e.g., 'summarize', 'extract price', 'convert to table'). Return ONLY the interpreted content. Do not add conversational filler."
        val userMessage = "Instruction: $instruction\n\nWeb Content:\n$content"

        try {
            val response =
                    api.getCompletion(
                            ChatRequest(
                                    model = model,
                                    messages =
                                            listOf(
                                                    Message("system", systemPrompt),
                                                    Message("user", userMessage)
                                            ),
                                    plugins = if (useWebSearch) listOf(Plugin("web")) else null
                            )
                    )
            val result = response.choices.firstOrNull()?.message?.content?.trim() ?: content
            logRepository?.logInfo("LLM", "interpretContent success, length: ${result.length}")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository?.logError(
                    "LLM",
                    "interpretContent failed: ${e.message}",
                    e.stackTraceToString()
            )
            return content // Fallback to original
        }
    }

    suspend fun testConnection(apiKey: String, model: String): String {
        // ... (Keep existing implementation)
        logRepository?.logInfo("LLM", "Testing connection to OpenRouter...")
        val client =
                OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request =
                                    chain.request()
                                            .newBuilder()
                                            .addHeader("Authorization", "Bearer $apiKey")
                                            .addHeader(
                                                    "HTTP-Referer",
                                                    "https://github.com/example/webpursuer"
                                            )
                                            .addHeader("X-Title", "WebPursuer")
                                            .build()
                            chain.proceed(request)
                        }
                        .build()

        val retrofit =
                Retrofit.Builder()
                        .baseUrl("https://openrouter.ai/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        try {
            val response =
                    api.getCompletion(
                            ChatRequest(
                                    model = model,
                                    messages = listOf(Message("user", "Say hello"))
                            )
                    )
            val result = response.choices.firstOrNull()?.message?.content ?: "No response content"
            logRepository?.logInfo("LLM", "Test connection result: $result")
            return result
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMsg = "Error: ${e.code()} - $errorBody"
            logRepository?.logError("LLM", "Test connection failed: $errorMsg")
            return errorMsg
        } catch (e: Exception) {
            logRepository?.logError("LLM", "Test connection failed: ${e.message}")
            return "Error: ${e.message}"
        }
    }

    suspend fun generateReport(prompt: String, useWebSearch: Boolean = false): String {
        logRepository?.logInfo("LLM", "Generating report...")
        val apiKey = settingsRepository.apiKey.first() ?: return "Error: No API Key"
        val model = settingsRepository.reportModel.first() // Use Report Model

        val client =
                OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request =
                                    chain.request()
                                            .newBuilder()
                                            .addHeader("Authorization", "Bearer $apiKey")
                                            .addHeader(
                                                    "HTTP-Referer",
                                                    "https://github.com/example/webpursuer"
                                            )
                                            .addHeader("X-Title", "WebPursuerReport")
                                            .build()
                            chain.proceed(request)
                        }
                        .build()
        val retrofit =
                Retrofit.Builder()
                        .baseUrl("https://openrouter.ai/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        val systemPrompt =
                "You are a professional News Reporter. Your job is to write a detailed, engaging, and accurate news report based on the provided updates from monitored websites. For each website change, compare the 'OLD CONTENT' and 'NEW CONTENT' to identify exactly what changed (prices, text, availability, etc.). Write a narrative summary of these changes. Do not just list them; explain them. If there are multiple updates, group them logically."

        try {
            val response =
                    api.getCompletion(
                            ChatRequest(
                                    model = model,
                                    messages =
                                            listOf(
                                                    Message("system", systemPrompt),
                                                    Message("user", prompt)
                                            ),
                                    plugins = if (useWebSearch) listOf(Plugin("web")) else null
                            )
                    )
            val content =
                    response.choices.firstOrNull()?.message?.content ?: "No summary available."
            logRepository?.logInfo("LLM", "Report generated successfully.")
            return content
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository?.logError(
                    "LLM",
                    "Error generating report: ${e.message}",
                    e.stackTraceToString()
            )
            return "Error generating report: ${e.message}"
        }
    }
}
