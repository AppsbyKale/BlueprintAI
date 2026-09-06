package com.example.blueprintai.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiContent(val role: String? = null, val parts: List<GeminiPart>)

@Serializable
data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
data class GeminiPartResponse(val text: String? = null)

@Serializable
data class GeminiContentResponse(val parts: List<GeminiPartResponse>? = null)

@Serializable
data class GeminiCandidate(val content: GeminiContentResponse? = null)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

class GeminiModelClient(
    private val apiKey: String,
    private val httpClient: HttpClient
) : ModelClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = flow {
        if (apiKey.isBlank()) {
            emit("Error: Gemini API key is missing. Please add your key in Settings (3-dot menu -> AI Models).")
            return@flow
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
            val contents = messages.map { msg ->
                GeminiContent(
                    role = if (msg.role == "assistant") "model" else "user",
                    parts = listOf(GeminiPart(text = msg.content))
                )
            }
            val requestBody = GeminiRequest(contents = contents)

            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.value in 200..299) {
                val responseText = response.bodyAsText()
                val geminiResp = json.decodeFromString<GeminiResponse>(responseText)
                val reply = geminiResp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    emit(reply)
                } else {
                    emit("Error: Received empty response from Gemini API.")
                }
            } else {
                emit("Error: Gemini API returned status ${response.status.value}: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            emit("Error calling Gemini API: ${e.localizedMessage}")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun getContextCapacity(): Int = 1048576
}
