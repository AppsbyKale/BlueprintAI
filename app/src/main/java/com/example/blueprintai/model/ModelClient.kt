package com.example.blueprintai.model

import kotlinx.coroutines.flow.Flow

interface ModelClient {
    fun generateResponse(prompt: String): Flow<String> = generateChatResponse(listOf(ChatMessage(role = "user", content = prompt)))
    fun generateChatResponse(messages: List<ChatMessage>): Flow<String>
    suspend fun isAvailable(): Boolean
    suspend fun getContextCapacity(): Int = 4096
}
