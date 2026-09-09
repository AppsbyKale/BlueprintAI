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

class RemoteModelClient(
    private val localIpUrl: String,
    private val publicIpUrl: String = "",
    private val apiKey: String = "",
    private val httpClient: HttpClient
) : ModelClient {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun resolveReachableUrl(): String = withContext(Dispatchers.IO) {
        val cleanLocal = localIpUrl.trim().trimEnd('/')
        if (cleanLocal.isNotEmpty() && checkPing(cleanLocal)) {
            return@withContext cleanLocal
        }

        val cleanPublic = publicIpUrl.trim().trimEnd('/')
        if (cleanPublic.isNotEmpty() && checkPing(cleanPublic)) {
            return@withContext cleanPublic
        }

        return@withContext if (cleanLocal.isNotEmpty()) cleanLocal else cleanPublic
    }

    private suspend fun checkPing(baseUrl: String): Boolean = try {
        val response = httpClient.get("$baseUrl/models") {
            if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
        }
        response.status.value in 200..399
    } catch (e: Exception) {
        false
    }

    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = flow {
        try {
            val cleanUrl = resolveReachableUrl()
            val response = httpClient.post("$cleanUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
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
        val cleanLocal = localIpUrl.trim().trimEnd('/')
        val cleanPublic = publicIpUrl.trim().trimEnd('/')
        return@withContext (cleanLocal.isNotEmpty() && checkPing(cleanLocal)) || (cleanPublic.isNotEmpty() && checkPing(cleanPublic))
    }

    override suspend fun getContextCapacity(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanUrl = resolveReachableUrl()
            val modelsResp = httpClient.get("$cleanUrl/models") {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (modelsResp.status.value in 200..299) {
                val text = modelsResp.bodyAsText()
                val parsed = json.decodeFromString<ModelsListResponse>(text)
                val modelObj = parsed.data?.firstOrNull() ?: parsed.models?.firstOrNull()

                val explicitLen = modelObj?.context_length ?: modelObj?.max_model_len ?: modelObj?.context_window
                if (explicitLen != null && explicitLen > 1024) {
                    return@withContext explicitLen
                }

                val modelName = (modelObj?.id ?: modelObj?.name ?: "").lowercase()
                when {
                    modelName.contains("128k") || modelName.contains("llama-3") || modelName.contains("gpt-4") || modelName.contains("claude") -> 131072
                    modelName.contains("32k") || modelName.contains("qwen") || modelName.contains("mistral") -> 32768
                    modelName.contains("16k") -> 16384
                    modelName.contains("8k") || modelName.contains("gemma") -> 8192
                    else -> 26000
                }
            } else {
                26000
            }
        } catch (e: Exception) {
            26000
        }
    }
}
