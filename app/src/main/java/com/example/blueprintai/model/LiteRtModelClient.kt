package com.example.blueprintai.model

import android.content.Context
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class LiteRtModelClient(
    private val context: Context,
    private val modelPath: String
) : ModelClient {

    @Volatile private var engine: Engine? = null
    private val mutex = Mutex()

    @OptIn(ExperimentalApi::class)
    private suspend fun getOrInitEngine(): Engine = withContext(Dispatchers.Default) {
        engine?.let { return@withContext it }
        mutex.withLock {
            engine?.let { return@withLock it }
            
            ExperimentalFlags.enableSpeculativeDecoding = true
            val file = File(modelPath)

            val loadedEngine = try {
                initializeWith(file, Backend.GPU())
            } catch (e: Exception) {
                try {
                    initializeWith(file, Backend.CPU())
                } catch (e2: Exception) {
                    throw e2
                }
            }
            engine = loadedEngine
            loadedEngine
        }
    }

    private fun initializeWith(model: File, backend: Backend): Engine {
        val config = EngineConfig(
            modelPath = model.absolutePath,
            backend = backend,
            cacheDir = context.cacheDir.path
        )
        return Engine(config).also { created ->
            created.initialize()
        }
    }

    override fun generateChatResponse(messages: List<ChatMessage>): Flow<String> = callbackFlow {
        val currentEngine = try {
            getOrInitEngine()
        } catch (e: Throwable) {
            val msg = e.localizedMessage ?: e.message ?: "Failed to initialize LiteRT-LM Engine"
            trySend(
                "Error loading LiteRT-LM model at path:\n'$modelPath'\n\n" +
                "Details: $msg\n\n" +
                "💡 Tip: Ensure the file is a valid Gemma-4-E2B-it.litertlm / LiteRT model file."
            )
            close()
            return@callbackFlow
        }

        var conversation: Conversation? = null
        try {
            // Fast sampler config optimized for mobile hardware
            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of("You are a helpful AI assistant."),
                samplerConfig = SamplerConfig(topK = 20, topP = 0.8, temperature = 0.3),
                maxOutputToken = 512
            )
            conversation = currentEngine.createConversation(conversationConfig)

            // Format multi-turn chat history into prompt for local LiteRT-LM model
            val formattedPrompt = messages.takeLast(10).joinToString("\n") { msg ->
                val roleName = if (msg.role == "user") "User" else "Assistant"
                "$roleName: ${msg.content}"
            } + "\nAssistant:"

            conversation.sendMessageAsync(Contents.of(formattedPrompt))
                .map { message ->
                    message.contents.contents
                        .asSequence()
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                }
                .collect { chunk ->
                    trySend(chunk)
                }
        } catch (e: Throwable) {
            trySend("Error generating response: ${e.localizedMessage ?: e.message}")
        } finally {
            runCatching { conversation?.close() }
            close()
        }

        awaitClose { /* No-op */ }
    }.flowOn(Dispatchers.Default)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(modelPath)
            file.exists() && file.length() > 1024 * 1024 // > 1MB
        } catch (e: Exception) {
            false
        }
    }
}
