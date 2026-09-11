package com.example.blueprintai.testing.unit

import app.cash.turbine.test
import com.example.blueprintai.data.*
import com.example.blueprintai.domain.repository.IChatRepository
import com.example.blueprintai.domain.repository.IFolderRepository
import com.example.blueprintai.ui.ChatViewModel
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
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chatRepository: IChatRepository = mockk(relaxed = true)
    private val folderRepository: IFolderRepository = mockk(relaxed = true)
    private val attachmentManager: AttachmentManager = mockk(relaxed = true)
    private val voiceManager: VoiceManager = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val mockFolders = listOf(Folder(id = 1L, name = "General"))
        coEvery { folderRepository.getFolders() } returns flowOf(mockFolders)
        coEvery { settingsDao.getSettings() } returns flowOf(Settings())
        coEvery { chatRepository.getMessages(any()) } returns flowOf(emptyList())
        coEvery { chatRepository.getAttachmentsForFolder(any()) } returns flowOf(emptyList())

        viewModel = ChatViewModel(
            repository = chatRepository,
            folderRepository = folderRepository,
            attachmentManager = attachmentManager,
            voiceManager = voiceManager,
            settingsDao = settingsDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectFolder updates currentFolderId state flow`() = runTest {
        viewModel.selectFolder(5L)

        viewModel.currentFolderId.test {
            assertEquals(5L, awaitItem())
        }
    }

    @Test
    fun `setSearchQuery updates searchQuery state flow`() = runTest {
        viewModel.setSearchQuery("architecture")

        viewModel.searchQuery.test {
            assertEquals("architecture", awaitItem())
        }
    }
}
