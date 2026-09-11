package com.example.blueprintai.testing.unit

import android.content.Context
import com.example.blueprintai.data.LogManager
import com.example.blueprintai.data.RemoteModelProfileDao
import com.example.blueprintai.data.SecureStorage
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import com.example.blueprintai.model.FallbackModelClient
import com.example.blueprintai.model.GeminiModelClient
import com.example.blueprintai.model.ModelManager
import com.example.blueprintai.model.RemoteModelClient
import io.ktor.client.HttpClient
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ModelManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val remoteModelProfileDao: RemoteModelProfileDao = mockk(relaxed = true)
    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val httpClient: HttpClient = mockk(relaxed = true)
    private val logManager: LogManager = mockk(relaxed = true)

    private lateinit var modelManager: ModelManager

    @Before
    fun setUp() {
        modelManager = ModelManager(
            context = context,
            settingsDao = settingsDao,
            remoteModelProfileDao = remoteModelProfileDao,
            secureStorage = secureStorage,
            httpClient = httpClient,
            logManager = logManager
        )
    }

    @Test
    fun `getLocalFallbackClient returns GeminiModelClient when Gemini key present`() = runTest {
        coEvery { settingsDao.getSettings() } returns flowOf(Settings(localModelPath = "", geminiApiKey = ""))
        coEvery { secureStorage.getString(SecureStorage.KEY_GEMINI_API_KEY) } returns "mock-gemini-key-123"

        val client = modelManager.getLocalFallbackClient()

        assertTrue(client is GeminiModelClient)
    }

    @Test
    fun `getLocalFallbackClient returns FallbackModelClient when no local path or Gemini key configured`() = runTest {
        coEvery { settingsDao.getSettings() } returns flowOf(Settings(localModelPath = "", geminiApiKey = ""))
        coEvery { secureStorage.getString(SecureStorage.KEY_GEMINI_API_KEY) } returns ""

        val client = modelManager.getLocalFallbackClient()

        assertTrue(client is FallbackModelClient)
    }

    @Test
    fun `getActiveClient resolves Remote client when profile present and mode is Remote`() = runTest {
        coEvery { settingsDao.getSettings() } returns flowOf(Settings(modelMode = "Remote"))
        coEvery { remoteModelProfileDao.getActiveProfile() } returns mockk(relaxed = true) {
            every { localIpUrl } returns "http://192.168.1.50:1234/v1"
            every { activeIpMode } returns "LOCAL"
            every { id } returns 1L
            every { modelName } returns "gemma-2"
        }
        coEvery { secureStorage.getString(any()) } returns ""

        val client = modelManager.getActiveClient()

        assertTrue(client is RemoteModelClient)
    }
}
