package com.example.blueprintai.model

import kotlinx.coroutines.flow.Flow

interface ModelClient {
    fun generateResponse(prompt: String): Flow<String>
    suspend fun isAvailable(): Boolean
}
