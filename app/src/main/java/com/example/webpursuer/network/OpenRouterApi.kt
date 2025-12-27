package com.example.webpursuer.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun getCompletion(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val plugins: List<Plugin>? = null
)

data class Plugin(val id: String, val max_results: Int? = null)

data class Message(val role: String, val content: String)

data class ChatResponse(val choices: List<Choice>)

data class Choice(val message: Message)
