package com.example.blueprintai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprintai.data.BackupManager
import com.example.blueprintai.data.LogManager
import com.example.blueprintai.data.ModelDownloader
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.data.RemoteModelProfileDao
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import com.example.blueprintai.model.RemoteModelClient
import io.ktor.client.HttpClient
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val remoteModelProfileDao: RemoteModelProfileDao,
    private val backupManager: BackupManager,
    private val logManager: LogManager,
    private val modelDownloader: ModelDownloader,
    private val httpClient: HttpClient
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsDao.getSettings()
        .map { it ?: Settings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    val remoteProfiles: StateFlow<List<RemoteModelProfile>> = remoteModelProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgress = modelDownloader.downloadProgress

    val logs = logManager.logs

    val modelStatus: StateFlow<String> = logs.map { list ->
        list.lastOrNull { it.tag == "Model" || it.tag == "Attachment" }?.message ?: "System Ready"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Initializing...")

    init {
        viewModelScope.launch {
            val list = remoteModelProfileDao.getAllProfiles().first()
            if (list.isEmpty()) {
                val initialUrl = settings.value.desktopUrl.ifBlank { "http://192.168.1.10:1234/v1" }
                val defaultProfile = RemoteModelProfile(
                    label = "Home Desktop (LM Studio)",
                    localIpUrl = cleanDesktopUrl(initialUrl),
                    publicIpUrl = "",
                    isActive = true
                )
                remoteModelProfileDao.insertProfile(defaultProfile)
            }
        }
    }

    fun updateModelMode(mode: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(modelMode = mode))
        }
    }

    fun saveAiModelSettings(localPath: String, desktopUrl: String, geminiKey: String) {
        viewModelScope.launch {
            val current = settings.value
            val cleanUrl = if (desktopUrl.isNotBlank()) cleanDesktopUrl(desktopUrl) else current.desktopUrl
            
            settingsDao.saveSettings(
                current.copy(
                    localModelPath = localPath,
                    desktopUrl = cleanUrl,
                    geminiApiKey = geminiKey
                )
            )

            if (cleanUrl.isNotBlank()) {
                val activeProfile = remoteModelProfileDao.getActiveProfile()
                if (activeProfile != null) {
                    remoteModelProfileDao.updateProfile(
                        activeProfile.copy(localIpUrl = cleanUrl)
                    )
                } else {
                    remoteModelProfileDao.insertProfile(
                        RemoteModelProfile(
                            label = "Home Desktop",
                            localIpUrl = cleanUrl,
                            isActive = true
                        )
                    )
                }
            }
        }
    }

    suspend fun fetchRemoteModels(url: String, apiKey: String = ""): List<String> {
        val client = RemoteModelClient(targetUrl = url, apiKey = apiKey, httpClient = httpClient)
        return client.fetchAvailableModels()
    }

    fun addRemoteProfile(label: String, localIpUrl: String, publicIpUrl: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            remoteModelProfileDao.clearActiveProfiles()
            val cleanLocal = cleanDesktopUrl(localIpUrl)
            val cleanPublic = if (publicIpUrl.isNotBlank()) cleanDesktopUrl(publicIpUrl) else ""
            val profile = RemoteModelProfile(
                label = label,
                localIpUrl = cleanLocal,
                publicIpUrl = cleanPublic,
                modelName = modelName.trim(),
                apiKey = apiKey,
                isActive = true
            )
            remoteModelProfileDao.insertProfile(profile)
            
            // Sync with settings desktopUrl
            val current = settings.value
            settingsDao.saveSettings(current.copy(desktopUrl = cleanLocal))
        }
    }

    fun updateRemoteProfile(profile: RemoteModelProfile) {
        viewModelScope.launch {
            val cleanLocal = cleanDesktopUrl(profile.localIpUrl)
            val cleanPublic = if (profile.publicIpUrl.isNotBlank()) cleanDesktopUrl(profile.publicIpUrl) else ""
            val updated = profile.copy(
                localIpUrl = cleanLocal,
                publicIpUrl = cleanPublic
            )
            remoteModelProfileDao.updateProfile(updated)

            if (profile.isActive) {
                val current = settings.value
                settingsDao.saveSettings(current.copy(desktopUrl = cleanLocal))
            }
        }
    }

    fun deleteRemoteProfile(profile: RemoteModelProfile) {
        viewModelScope.launch {
            remoteModelProfileDao.deleteProfile(profile)
        }
    }

    fun setActiveRemoteProfile(profileId: Long) {
        viewModelScope.launch {
            remoteModelProfileDao.switchActiveProfile(profileId)
            val active = remoteModelProfileDao.getActiveProfile()
            if (active != null) {
                val current = settings.value
                settingsDao.saveSettings(current.copy(desktopUrl = active.localIpUrl))
            }
        }
    }

    fun setProfileIpMode(profileId: Long, mode: String) {
        viewModelScope.launch {
            remoteModelProfileDao.setProfileIpMode(profileId, mode)
        }
    }

    fun updateLocalPath(path: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(localModelPath = path))
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(geminiApiKey = key))
        }
    }

    fun toggleTts(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(isTtsEnabled = enabled))
        }
    }

    fun createBackup(file: File) {
        viewModelScope.launch {
            backupManager.createBackup(file)
        }
    }

    fun restoreBackup(file: File) {
        viewModelScope.launch {
            backupManager.restoreBackup(file)
        }
    }

    fun clearLogs() {
        logManager.clearLogs()
    }

    fun exportLogs(): String = logManager.exportTrainingData()

    fun downloadGemmaModel(
        url: String = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        targetPath: String = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
    ) {
        viewModelScope.launch {
            modelDownloader.downloadModel(url, targetPath)
            if (modelDownloader.downloadProgress.value.isCompleted) {
                updateLocalPath(targetPath)
            }
        }
    }

    fun resetDownloadProgress() {
        modelDownloader.resetProgress()
    }
}
