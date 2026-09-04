package com.example.blueprintai.model

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
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
            val cleanUrl = baseUrl.trim().trimEnd('/')
            val response = httpClient.post("$cleanUrl/chat/completions") {
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
