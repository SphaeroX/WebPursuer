package com.example.webpursuer.network

import com.example.webpursuer.data.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenRouterService(private val settingsRepository: SettingsRepository) {

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        // We need to block here to get the key, or we could use a provider.
        // For simplicity in this worker context, we'll assume the key is available or handle failure.
        // Ideally, we shouldn't block main thread, but this will be called from Worker (bg thread).
        // However, runBlocking is risky. Better to pass the key to the function call or use a provider.
        // Let's use a dynamic interceptor approach or just recreate the client if key changes?
        // Simpler: The service method will take the key.
        // Actually, let's just add the header if we have it.
        
        // Wait, I can't easily access suspend function in interceptor.
        // I will pass the key when making the call or create the client with the key.
        // But the key is in DataStore.
        
        // Alternative: Create the Retrofit instance on demand or use a mutable holder.
        // Let's keep it simple: The `checkContent` function will take the key.
        // But Retrofit needs the client.
        
        // Let's make a function that creates the API instance given a key.
        chain.proceed(requestBuilder.build())
    }

    suspend fun checkContent(prompt: String, content: String): Boolean {
        val apiKey = settingsRepository.apiKey.first() ?: return false
        val model = settingsRepository.model.first()

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
            return reply == "YES"
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    suspend fun testConnection(apiKey: String, model: String): String {
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
            return response.choices.firstOrNull()?.message?.content ?: "No response content"
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            return "Error: ${e.code()} - $errorBody"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }

    suspend fun generateReport(prompt: String): String {
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

        // System prompt for the report
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
            return response.choices.firstOrNull()?.message?.content ?: "No summary available."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error generating report: ${e.message}"
        }
    }
}
