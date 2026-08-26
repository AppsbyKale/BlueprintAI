package com.example.blueprintai.model

import android.content.Context
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import io.ktor.client.*
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val httpClient: HttpClient,
    private val logManager: com.example.blueprintai.data.LogManager
) {
    suspend fun getActiveClient(): ModelClient {
        val currentSettings = settingsDao.getSettings().first() ?: Settings()
        logManager.log("INFO", "Model", "Initializing ${currentSettings.modelMode} mode")
        return when (currentSettings.modelMode) {
            "Phone" -> LiteRtModelClient(context, currentSettings.localModelPath)
            "Desktop" -> RemoteModelClient(currentSettings.desktopUrl, httpClient)
            "Auto" -> {
                val remote = RemoteModelClient(currentSettings.desktopUrl, httpClient)
                if (remote.isAvailable()) remote 
                else LiteRtModelClient(context, currentSettings.localModelPath)
            }
            else -> LiteRtModelClient(context, currentSettings.localModelPath)
        }
    }
}
