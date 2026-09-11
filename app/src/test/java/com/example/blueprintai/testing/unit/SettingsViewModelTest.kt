package com.example.blueprintai.testing.unit

import app.cash.turbine.test
import com.example.blueprintai.data.*
import com.example.blueprintai.ui.SettingsViewModel
import io.ktor.client.HttpClient
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val remoteModelProfileDao: RemoteModelProfileDao = mockk(relaxed = true)
    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val backupManager: BackupManager = mockk(relaxed = true)
    private val logManager: LogManager = mockk(relaxed = true)
    private val modelDownloader: ModelDownloader = mockk(relaxed = true)
    private val httpClient: HttpClient = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { settingsDao.getSettings() } returns flowOf(Settings(modelMode = "Auto"))
        coEvery { remoteModelProfileDao.getAllProfiles() } returns flowOf(emptyList())

        viewModel = SettingsViewModel(
            settingsDao = settingsDao,
            remoteModelProfileDao = remoteModelProfileDao,
            secureStorage = secureStorage,
            backupManager = backupManager,
            logManager = logManager,
            modelDownloader = modelDownloader,
            httpClient = httpClient
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings flow emits settings from dao`() = runTest {
        testScheduler.advanceUntilIdle()

        viewModel.settings.test {
            val item = awaitItem()
            assertEquals("Auto", item.modelMode)
        }
    }

    @Test
    fun `updateModelMode saves updated settings`() = runTest {
        testScheduler.advanceUntilIdle()

        viewModel.updateModelMode("Remote")
        testScheduler.advanceUntilIdle()

        coVerify { settingsDao.saveSettings(match { it.modelMode == "Remote" }) }
    }
}
