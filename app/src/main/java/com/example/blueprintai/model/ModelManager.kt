package com.example.blueprintai.model

import android.content.Context
import com.example.blueprintai.data.LogManager
import com.example.blueprintai.data.RemoteModelProfileDao
import com.example.blueprintai.data.SecureStorage
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import com.example.blueprintai.domain.model.IpMode
import com.example.blueprintai.domain.model.ModelMode
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
    private val secureStorage: SecureStorage,
    private val httpClient: HttpClient,
    private val logManager: LogManager
) {
    suspend fun getActiveClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        val mode = ModelMode.fromKey(currentSettings.modelMode)

        logManager.log("INFO", "Model", "Initializing ${mode.key} mode")

        return when (mode) {
            ModelMode.REMOTE -> {
                val remote = createRemoteClient()
                if (remote != null && remote.getCleanUrl().isNotBlank()) {
                    remote
                } else {
                    logManager.log("INFO", "Model", "No valid Remote profile, falling back to Local model")
                    getLocalFallbackClient()
                }
            }
            ModelMode.LOCAL -> {
                getLocalFallbackClient()
            }
            ModelMode.AUTO -> {
                val remote = createRemoteClient()
                if (remote != null && remote.isAvailable()) {
                    logManager.log("INFO", "Model", "Auto mode selected Remote model profile")
                    remote
                } else {
                    logManager.log("INFO", "Model", "Auto mode selected Local model")
                    getLocalFallbackClient()
                }
            }
        }
    }

    private suspend fun createRemoteClient(): RemoteModelClient? {
        val activeProfile = remoteModelProfileDao.getActiveProfile()
            ?: remoteModelProfileDao.getAllProfiles().first().firstOrNull()?.also {
                remoteModelProfileDao.setActiveProfileById(it.id)
            }

        return if (activeProfile != null) {
            val ipMode = IpMode.fromKey(activeProfile.activeIpMode)
            val selectedUrl = if (ipMode == IpMode.PUBLIC && activeProfile.publicIpUrl.isNotBlank()) {
                activeProfile.publicIpUrl
            } else {
                activeProfile.localIpUrl
            }

            val apiKey = secureStorage.getString(SecureStorage.profileApiKey(activeProfile.id))
                .ifBlank { activeProfile.apiKey }

            logManager.log("INFO", "Model", "Using active profile '${activeProfile.label}' with ${activeProfile.activeIpMode} IP ($selectedUrl)")

            RemoteModelClient(
                targetUrl = selectedUrl,
                selectedModel = activeProfile.modelName,
                apiKey = apiKey,
                httpClient = httpClient
            )
        } else {
            null
        }
    }

    suspend fun getLocalFallbackClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        val geminiApiKey = secureStorage.getString(SecureStorage.KEY_GEMINI_API_KEY)
            .ifBlank { currentSettings.geminiApiKey }

        return if (currentSettings.localModelPath.isNotBlank() && File(currentSettings.localModelPath).exists()) {
            logManager.log("INFO", "Model", "Using Local LiteRT model")
            LiteRtModelClient(context, currentSettings.localModelPath)
        } else if (geminiApiKey.isNotBlank()) {
            logManager.log("INFO", "Model", "Using Gemini API")
            GeminiModelClient(geminiApiKey, httpClient)
        } else {
            FallbackModelClient("No local model (.litertlm) file or Gemini API key configured. Please open Settings (3-dot menu -> AI Models) to download Gemma 4-E2B or enter a Gemini API key.")
        }
    }
}
