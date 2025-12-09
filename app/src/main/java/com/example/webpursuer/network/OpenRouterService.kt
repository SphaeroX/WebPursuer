package com.example.webpursuer.network

import com.example.webpursuer.data.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenRouterService(
    private val settingsRepository: SettingsRepository,
    private val logRepository: com.example.webpursuer.data.LogRepository? = null
) {
    // ... (Interceptor logic omitted for brevity, keeping existing)

    suspend fun checkContent(prompt: String, content: String): Boolean {
        logRepository?.logInfo("LLM", "checkContent called request for prompt: ${prompt.take(50)}...")
        val apiKey = settingsRepository.apiKey.first() ?: return false
        val model = settingsRepository.model.first()

        // ... (Client building)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/example/webpursuer") // Required by OpenRouter
                    .addHeader("X-Title", "WebPursuer")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        val systemPrompt = "You are a helpful assistant that analyzes web content. You will be given a user prompt and the content of a webpage. You must decide if the user's condition is met. Reply ONLY with 'YES' or 'NO'."
        val userMessage = "User Condition: $prompt\n\nWeb Content:\n$content"

        try {
            val response = api.getCompletion(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userMessage)
                    )
                )
            )
            
            val reply = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase()
            logRepository?.logInfo("LLM", "checkContent response: $reply")
            return reply == "YES"
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository?.logError("LLM", "checkContent failed: ${e.message}", e.stackTraceToString())
            return false
        }
    }

    suspend fun testConnection(apiKey: String, model: String): String {
        logRepository?.logInfo("LLM", "Testing connection to OpenRouter...")
        // ... (Client building details identical to existing)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/example/webpursuer") 
                    .addHeader("X-Title", "WebPursuer")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        try {
            val response = api.getCompletion(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        Message("user", "Say hello")
                    )
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

    suspend fun generateReport(prompt: String): String {
        logRepository?.logInfo("LLM", "Generating report...")
        val apiKey = settingsRepository.apiKey.first() ?: return "Error: No API Key"
        val model = settingsRepository.model.first()

        val client = OkHttpClient.Builder()
             .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/example/webpursuer")
                    .addHeader("X-Title", "WebPursuerReport")
                    .build()
                chain.proceed(request)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        val systemPrompt = "You are a helpful assistant that summarizes changes on monitored websites. You will be given a list of changes including the website name, check time, and the change detected. Create a concise summary of what happened. If multiple updates are for the same site, group them."

        try {
            val response = api.getCompletion(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", prompt)
                    )
                )
            )
            val content = response.choices.firstOrNull()?.message?.content ?: "No summary available."
            logRepository?.logInfo("LLM", "Report generated successfully.")
            return content
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository?.logError("LLM", "Error generating report: ${e.message}", e.stackTraceToString())
            return "Error generating report: ${e.message}"
        }
    }
}
