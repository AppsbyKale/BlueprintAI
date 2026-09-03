package com.example.blueprintai.model

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class LiteRtModelClient(
    private val context: Context,
    private val modelPath: String
) : ModelClient {

    private var llmInference: LlmInference? = null

    private fun getInference(): LlmInference {
        return llmInference ?: synchronized(this) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setTemperature(0.7f)
                .build()
            LlmInference.createFromOptions(context, options).also { llmInference = it }
        }
    }

    override fun generateResponse(prompt: String): Flow<String> = callbackFlow {
        val inference = try {
            getInference()
        } catch (e: Throwable) {
            val msg = e.localizedMessage ?: e.message ?: "Invalid or incompatible model file format"
            trySend(
                "Error loading local model at path:\n'$modelPath'\n\n" +
                "Details: $msg\n\n" +
                "💡 Tip: MediaPipe GenAI requires a compiled MediaPipe .bin or .task model file (e.g., Gemma 2b / Gemma 3 in .task format). " +
                "You can also configure a Gemini API Key in Settings (3-dot menu -> AI Models) for cloud generation."
            )
            close()
            return@callbackFlow
        }
        
        try {
            val result = inference.generateResponse(prompt)
            trySend(result)
        } catch (e: Throwable) {
            trySend("Error generating response: ${e.localizedMessage ?: e.message}")
        } finally {
            close()
        }
        
        awaitClose { /* No-op */ }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            File(modelPath).exists()
        } catch (e: Exception) {
            false
        }
    }
}
