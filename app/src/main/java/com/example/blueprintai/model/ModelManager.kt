package com.example.blueprintai.model

import android.content.Context
import com.example.blueprintai.data.LogManager
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
                    val remote = RemoteModelClient(currentSettings.desktopUrl, httpClient)
                    if (remote.isAvailable()) remote
                    else FallbackModelClient("Error: Unable to connect to Desktop server at ${currentSettings.desktopUrl}.\nPlease check if your desktop server (e.g. LM Studio / Ollama) is running.")
                }
            }
            "Phone" -> {
                if (currentSettings.localModelPath.isNotBlank() && File(currentSettings.localModelPath).exists()) {
                    LiteRtModelClient(context, currentSettings.localModelPath)
                } else if (currentSettings.geminiApiKey.isNotBlank()) {
                    GeminiModelClient(currentSettings.geminiApiKey, httpClient)
                } else {
                    FallbackModelClient("Error: Local model file not found at:\n'${currentSettings.localModelPath}'\n\nPlease check Settings (3-dot menu -> AI Models) to configure a valid model path or add a Gemini API Key.")
                }
            }
            "Auto" -> {
                if (currentSettings.isRemoteEnabled) {
                    val remote = RemoteModelClient(currentSettings.desktopUrl, httpClient)
                    if (remote.isAvailable()) {
                        logManager.log("INFO", "Model", "Auto mode selected Desktop model")
                        return remote
                    }
                }
                
                if (currentSettings.geminiApiKey.isNotBlank()) {
                    logManager.log("INFO", "Model", "Auto mode selected Gemini Cloud API")
                    GeminiModelClient(currentSettings.geminiApiKey, httpClient)
                } else if (currentSettings.localModelPath.isNotBlank() && File(currentSettings.localModelPath).exists()) {
                    logManager.log("INFO", "Model", "Auto mode selected Local LiteRT model")
                    LiteRtModelClient(context, currentSettings.localModelPath)
                } else {
                    FallbackModelClient("No AI Model Configured.\n\nPlease open Settings (3-dot menu -> AI Models) to configure:\n1. A Gemini API Key (cloud AI)\n2. A Desktop Server URL (e.g. LM Studio)\n3. A local MediaPipe .task / .bin model file")
                }
            }
            else -> {
                if (currentSettings.geminiApiKey.isNotBlank()) {
                    GeminiModelClient(currentSettings.geminiApiKey, httpClient)
                } else {
                    LiteRtModelClient(context, currentSettings.localModelPath)
                }
            }
        }
    }
}
