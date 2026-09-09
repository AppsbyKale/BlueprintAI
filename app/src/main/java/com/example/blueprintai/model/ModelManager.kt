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
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val remoteModelProfileDao: RemoteModelProfileDao,
    private val httpClient: HttpClient,
    private val logManager: LogManager
) {
    suspend fun getActiveClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        logManager.log("INFO", "Model", "Initializing ${currentSettings.modelMode} mode (Remote Enabled: ${currentSettings.isRemoteEnabled})")

        return when (currentSettings.modelMode) {
            "Desktop" -> {
                if (!currentSettings.isRemoteEnabled) {
                    FallbackModelClient("Remote AI Connection is currently suspended in Settings. Re-enable 'Remote Desktop AI' in the 3-dot menu to connect.")
                } else {
                    val remote = createRemoteClient(currentSettings)
                    if (remote.isAvailable()) remote
                    else FallbackModelClient("Error: Unable to connect to Desktop server.\nPlease check if your desktop server or active profile is running.")
                }
            }
            "Phone" -> {
                getLocalFallbackClient()
            }
            "Auto" -> {
                if (currentSettings.isRemoteEnabled) {
                    val remote = createRemoteClient(currentSettings)
                    if (remote.isAvailable()) {
                        logManager.log("INFO", "Model", "Auto mode selected Desktop model")
                        return remote
                    }
                }
                
                getLocalFallbackClient()
            }
            else -> {
                getLocalFallbackClient()
            }
        }
    }

    private suspend fun createRemoteClient(settings: Settings): RemoteModelClient {
        val activeProfile = remoteModelProfileDao.getActiveProfile()
        return if (activeProfile != null) {
            RemoteModelClient(
                localIpUrl = activeProfile.localIpUrl,
                publicIpUrl = activeProfile.publicIpUrl,
                apiKey = activeProfile.apiKey,
                httpClient = httpClient
            )
        } else {
            RemoteModelClient(
                localIpUrl = settings.desktopUrl,
                publicIpUrl = "",
                apiKey = "",
                httpClient = httpClient
            )
        }
    }

    suspend fun getLocalFallbackClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        return if (currentSettings.localModelPath.isNotBlank() && File(currentSettings.localModelPath).exists()) {
            logManager.log("INFO", "Model", "Using Local LiteRT model fallback")
            LiteRtModelClient(context, currentSettings.localModelPath)
        } else if (currentSettings.geminiApiKey.isNotBlank()) {
            logManager.log("INFO", "Model", "Using Gemini API fallback")
            GeminiModelClient(currentSettings.geminiApiKey, httpClient)
        } else {
            FallbackModelClient("No local model or Gemini API Key configured for fallback.")
        }
    }
}
