package com.example.blueprintai.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String = "local-model",
    val messages: List<ChatMessage>,
    val stream: Boolean = true
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatChunk(
    val choices: List<ChoiceChunk>? = null,
    val content: String? = null
)

@Serializable
data class ChoiceChunk(
    val delta: DeltaChunk? = null,
    val message: DeltaChunk? = null,
    val text: String? = null
) {
    fun extractContent(): String? {
        val main = delta?.content 
            ?: delta?.text 
            ?: message?.content 
            ?: message?.text 
            ?: text
        if (!main.isNullOrEmpty()) return main

        val reasoning = delta?.reasoning_content 
            ?: delta?.reasoning 
            ?: message?.reasoning_content 
            ?: message?.reasoning
        return reasoning
    }
}

@Serializable
data class DeltaChunk(
    val content: String? = null,
    val text: String? = null,
    val reasoning_content: String? = null,
    val reasoning: String? = null
)

@Serializable
data class ModelInfo(
    val id: String? = null,
    val name: String? = null,
    val context_length: Int? = null,
    val max_model_len: Int? = null,
    val context_window: Int? = null
)

@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo>? = null,
    val models: List<ModelInfo>? = null
)
