package com.example.blueprintai.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val choices: List<ChoiceChunk>? = null
)

@Serializable
data class ChoiceChunk(
    val delta: DeltaChunk? = null,
    val message: DeltaChunk? = null,
    val text: String? = null
) {
    fun extractContent(): String? {
        return delta?.content ?: delta?.text ?: message?.content ?: message?.text ?: text
    }
}

@Serializable
data class DeltaChunk(
    val content: String? = null,
    val text: String? = null
)

class RemoteModelClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) : ModelClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = flow {
        try {
            val cleanUrl = baseUrl.trim().trimEnd('/')
            val response = httpClient.post("$cleanUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        messages = messages
                    )
                )
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            var emittedAnyText = false
            val rawBuffer = StringBuilder()

            while (!channel.isClosedForRead) {
                val line = channel.readLine()?.trim() ?: break
                if (line.isEmpty()) continue

                rawBuffer.append(line).append("\n")

                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = json.decodeFromString<ChatChunk>(data)
                        val text = chunk.choices?.firstOrNull()?.extractContent()
                        if (!text.isNullOrEmpty()) {
                            emit(text)
                            emittedAnyText = true
                        }
                    } catch (e: Exception) {
                        // Ignore intermediate JSON parse errors for SSE lines
                    }
                }
            }

            if (!emittedAnyText) {
                val fullRaw = rawBuffer.toString().trim()
                if (fullRaw.isNotEmpty()) {
                    try {
                        val fullChunk = json.decodeFromString<ChatChunk>(fullRaw)
                        val text = fullChunk.choices?.firstOrNull()?.extractContent()
                        if (!text.isNullOrEmpty()) {
                            emit(text)
                            emittedAnyText = true
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            if (!emittedAnyText) {
                emit("Error: Received empty response from remote server ($cleanUrl). Please check that a model is currently loaded in LM Studio or Ollama.")
            }
        } catch (e: Exception) {
            emit("Error connecting to remote model: ${e.localizedMessage ?: "Unknown error"}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanUrl = baseUrl.trim().trimEnd('/')
            val modelsResp = httpClient.get("$cleanUrl/models")
            if (modelsResp.status.value in 200..299) return@withContext true

            val rootResp = httpClient.get(cleanUrl)
            rootResp.status.value in 200..399
        } catch (e: Exception) {
            false
        }
    }
}
