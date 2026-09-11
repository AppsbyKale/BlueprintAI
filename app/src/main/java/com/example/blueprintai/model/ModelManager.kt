package com.example.blueprintai.model

import android.content.Context
import com.example.blueprintai.data.LogManager
import com.example.blueprintai.data.RemoteModelProfileDao
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import io.ktor.client.*
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val remoteModelProfileDao: RemoteModelProfileDao,
    private val httpClient: HttpClient,
    private val logManager: LogManager
) {
    suspend fun getActiveClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        val normalizedMode = when (currentSettings.modelMode) {
            "Desktop" -> "Remote"
            "Phone" -> "Local"
            else -> currentSettings.modelMode
        }

        logManager.log("INFO", "Model", "Initializing $normalizedMode mode")

        return when (normalizedMode) {
            "Remote" -> {
                val remote = createRemoteClient()
                if (remote != null && remote.getCleanUrl().isNotBlank()) {
                    remote
                } else {
                    logManager.log("INFO", "Model", "No valid Remote profile, falling back to Local model")
                    getLocalFallbackClient()
                }
            }
            "Local" -> {
                getLocalFallbackClient()
            }
            "Auto" -> {
                val remote = createRemoteClient()
                if (remote != null && remote.isAvailable()) {
                    logManager.log("INFO", "Model", "Auto mode selected Remote model profile")
                    remote
                } else {
                    logManager.log("INFO", "Model", "Auto mode selected Local model")
                    getLocalFallbackClient()
                }
            }
            else -> {
                getLocalFallbackClient()
            }
        }
    }

    private suspend fun createRemoteClient(): RemoteModelClient? {
        val activeProfile = remoteModelProfileDao.getActiveProfile()
            ?: remoteModelProfileDao.getAllProfiles().first().firstOrNull()?.also {
                remoteModelProfileDao.setActiveProfileById(it.id)
            }

        return if (activeProfile != null) {
            val selectedUrl = if (activeProfile.activeIpMode == "PUBLIC" && activeProfile.publicIpUrl.isNotBlank()) {
                activeProfile.publicIpUrl
            } else {
                activeProfile.localIpUrl
            }

            logManager.log("INFO", "Model", "Using active profile '${activeProfile.label}' with ${activeProfile.activeIpMode} IP ($selectedUrl)")

            RemoteModelClient(
                targetUrl = selectedUrl,
                apiKey = activeProfile.apiKey,
                httpClient = httpClient
            )
        } else {
            null
        }
    }

    suspend fun getLocalFallbackClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        return if (currentSettings.localModelPath.isNotBlank() && File(currentSettings.localModelPath).exists()) {
            logManager.log("INFO", "Model", "Using Local LiteRT model")
            LiteRtModelClient(context, currentSettings.localModelPath)
        } else if (currentSettings.geminiApiKey.isNotBlank()) {
            logManager.log("INFO", "Model", "Using Gemini API")
            GeminiModelClient(currentSettings.geminiApiKey, httpClient)
        } else {
            FallbackModelClient("No local model (.litertlm) file or Gemini API key configured. Please open Settings (3-dot menu -> AI Models) to download Gemma 4-E2B or enter a Gemini API key.")
        }
    }
}
