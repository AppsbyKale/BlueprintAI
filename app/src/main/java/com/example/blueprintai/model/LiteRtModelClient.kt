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
        } catch (e: Exception) {
            trySend("Error loading model: ${e.localizedMessage}")
            close()
            return@callbackFlow
        }
        
        try {
            val result = inference.generateResponse(prompt)
            trySend(result)
        } catch (e: Exception) {
            trySend("Error: ${e.localizedMessage}")
        } finally {
            close()
        }
        
        awaitClose { /* No-op */ }
    }

    override suspend fun isAvailable(): Boolean {
        return File(modelPath).exists()
    }
}
