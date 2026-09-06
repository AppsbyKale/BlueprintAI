package com.example.blueprintai.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FallbackModelClient(
    private val message: String
) : ModelClient {
    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = flow {
        emit(message)
    }

    override suspend fun isAvailable(): Boolean = false
}
