package com.example.blueprintai.model

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

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
    val choices: List<ChoiceChunk>
)

@Serializable
data class ChoiceChunk(
    val delta: DeltaChunk
)

@Serializable
data class DeltaChunk(
    val content: String? = null
)

class RemoteModelClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) : ModelClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun generateResponse(prompt: String): Flow<String> = flow {
        try {
            val response = httpClient.post("$baseUrl/chat/completions") {
                setBody(
                    ChatRequest(
                        messages = listOf(ChatMessage(role = "user", content = prompt))
                    )
                )
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6)
                    if (data == "[DONE]") break
                    try {
                        val chunk = json.decodeFromString<ChatChunk>(data)
                        chunk.choices.firstOrNull()?.delta?.content?.let {
                            emit(it)
                        }
                    } catch (e: Exception) {
                        // Ignore parse errors for specific chunks
                    }
                }
            }
        } catch (e: Exception) {
            emit("Error: ${e.localizedMessage}")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val url = URL(baseUrl)
            val response = httpClient.get("${url.protocol}://${url.host}:${url.port}/v1/models")
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
