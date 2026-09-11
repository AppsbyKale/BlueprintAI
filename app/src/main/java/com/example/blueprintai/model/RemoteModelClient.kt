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

class RemoteModelClient(
    val targetUrl: String,
    val selectedModel: String = "",
    private val apiKey: String = "",
    private val httpClient: HttpClient
) : ModelClient {

    private val json = Json { ignoreUnknownKeys = true }

    fun getCleanUrl(): String {
        val trimmed = targetUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        var url = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "http://$trimmed"
        } else {
            trimmed
        }
        if (!url.endsWith("/v1", ignoreCase = true)) {
            url = "$url/v1"
        }
        return url
    }

    private suspend fun checkPing(): Boolean {
        val cleanUrl = getCleanUrl()
        if (cleanUrl.isBlank()) return false

        // 1. Try $cleanUrl/models (e.g. http://192.168.1.50:1234/v1/models)
        try {
            val response = httpClient.get("$cleanUrl/models") {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (response.status.value in 200..405) return true
        } catch (e: Exception) {
            // Ignore
        }

        // 2. Try $baseUrl/models without /v1 (e.g. http://192.168.1.50:1234/models)
        val rootUrl = cleanUrl.removeSuffix("/v1").removeSuffix("/v1/")
        try {
            val response = httpClient.get("$rootUrl/models") {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (response.status.value in 200..405) return true
        } catch (e: Exception) {
            // Ignore
        }

        // 3. Try pinging base URL root (e.g. http://192.168.1.50:1234)
        try {
            val response = httpClient.get(rootUrl) {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (response.status.value in 200..405) return true
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }

    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = flow {
        try {
            val cleanUrl = getCleanUrl()
            val reqModel = selectedModel.ifBlank { "local-model" }
            val response = httpClient.post("$cleanUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                setBody(
                    ChatRequest(
                        model = reqModel,
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
                } else if (line.startsWith("{") && line.endsWith("}")) {
                    try {
                        val chunk = json.decodeFromString<ChatChunk>(line)
                        val text = chunk.choices?.firstOrNull()?.extractContent()
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
                val fullRaw = rawBuffer.toString().trim()
                if (fullRaw.isNotEmpty()) {
                    for (rawLine in fullRaw.lines()) {
                        val cleanLine = rawLine.removePrefix("data:").trim()
                        if (cleanLine.isNotBlank() && cleanLine != "[DONE]") {
                            try {
                                val chunk = json.decodeFromString<ChatChunk>(cleanLine)
                                val text = chunk.choices?.firstOrNull()?.extractContent()
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
            }

            if (!emittedAnyText) {
                emit("Error: Received empty response from remote server ($cleanUrl). Please check that a model is currently loaded in LM Studio or Ollama.")
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Unknown error"
            val tip = if (msg.contains("timeout", ignoreCase = true) || msg.contains("refused", ignoreCase = true) || msg.contains("Host", ignoreCase = true)) {
                "\n\n💡 Quick Troubleshooting Checklist:\n" +
                "• On Home Wi-Fi? Check LM Studio -> Server Settings -> Enable 'Serve on Local Network' (0.0.0.0).\n" +
                "• Windows Firewall? Ensure port (e.g. 1234 / 11434) is allowed in Inbound Firewall Rules on your PC.\n" +
                "• Away on Mobile Data? Open 3-dot menu in BlueprintAI -> Toggle IP to 'Public (Away)'."
            } else ""
            emit("Error connecting to remote model ($targetUrl): $msg$tip")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext checkPing()
    }

    suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        val cleanUrl = getCleanUrl()
        if (cleanUrl.isBlank()) return@withContext emptyList()

        val list = mutableListOf<String>()
        try {
            val response = httpClient.get("$cleanUrl/models") {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (response.status.value in 200..299) {
                val text = response.bodyAsText()
                val parsed = json.decodeFromString<ModelsListResponse>(text)
                val items = parsed.data ?: parsed.models ?: emptyList()
                for (item in items) {
                    val id = item.id ?: item.name
                    if (!id.isNullOrBlank()) list.add(id)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return@withContext list.distinct()
    }

    override suspend fun getContextCapacity(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanUrl = getCleanUrl()
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
