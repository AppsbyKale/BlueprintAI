package com.example.blueprintai.model

import android.content.Context
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import java.io.File

class LiteRtModelClient(
    private val context: Context,
    private val modelPath: String
) : ModelClient {

    @Volatile private var engine: Engine? = null

    @OptIn(ExperimentalApi::class)
    private fun getOrInitEngine(): Engine {
        engine?.let { return it }
        return synchronized(this) {
            engine?.let { return@synchronized it }
            
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

    override fun generateResponse(prompt: String): Flow<String> = callbackFlow {
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
            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of("You are a helpful AI assistant."),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
                maxOutputToken = 1024
            )
            conversation = currentEngine.createConversation(conversationConfig)

            conversation.sendMessageAsync(Contents.of(prompt))
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
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val file = File(modelPath)
            file.exists() && file.length() > 1024 * 1024 // > 1MB
        } catch (e: Exception) {
            false
        }
    }
}
