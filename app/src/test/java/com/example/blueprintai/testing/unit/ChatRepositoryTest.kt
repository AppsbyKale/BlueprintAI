package com.example.blueprintai.testing.unit

import com.example.blueprintai.data.*
import com.example.blueprintai.model.ChatMessage
import com.example.blueprintai.model.ModelClient
import com.example.blueprintai.model.ModelManager
import com.example.blueprintai.model.ToolInterceptor
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private val folderDao: FolderDao = mockk(relaxed = true)
    private val messageDao: MessageDao = mockk(relaxed = true)
    private val attachmentDao: AttachmentDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val modelManager: ModelManager = mockk(relaxed = true)
    private val toolInterceptor: ToolInterceptor = mockk(relaxed = true)
    private val mockModelClient: ModelClient = mockk(relaxed = true)

    private lateinit var chatRepository: ChatRepository

    @Before
    fun setUp() {
        coEvery { settingsDao.getSettings() } returns flowOf(Settings(modelMode = "Auto"))
        coEvery { modelManager.getActiveClient() } returns mockModelClient
        coEvery { mockModelClient.getContextCapacity() } returns 26000

        chatRepository = ChatRepository(
            folderDao = folderDao,
            messageDao = messageDao,
            attachmentDao = attachmentDao,
            settingsDao = settingsDao,
            modelManager = modelManager,
            toolInterceptor = toolInterceptor
        )
    }

    @Test
    fun `getMessages returns flow from messageDao`() = runTest {
        val folderId = 1L
        val mockMessages = listOf(
            Message(id = 10, folderId = folderId, content = "Hello", role = "user"),
            Message(id = 11, folderId = folderId, content = "Hi there", role = "assistant")
        )
        coEvery { messageDao.getMessagesByFolder(folderId) } returns flowOf(mockMessages)

        val result = chatRepository.getMessages(folderId).first()

        assertEquals(2, result.size)
        assertEquals("Hello", result[0].content)
        assertEquals("Hi there", result[1].content)
    }

    @Test
    fun `sendMessage emits chunks and inserts assistant message when no tool call`() = runTest {
        val folderId = 1L
        val userPrompt = "What is Kotlin?"
        val aiResponseChunks = listOf("Kotlin is ", "a modern ", "programming language.")

        coEvery { messageDao.getMessagesByFolder(folderId) } returns flowOf(emptyList())
        coEvery { mockModelClient.generateChatResponse(any()) } returns aiResponseChunks.asFlow()
        coEvery { messageDao.insertMessage(any()) } returns 100L

        val emittedChunks = mutableListOf<String>()
        chatRepository.sendMessage(folderId, userPrompt).collect { chunk ->
            emittedChunks.add(chunk)
        }

        assertEquals(aiResponseChunks, emittedChunks)
        coVerify { messageDao.insertMessage(match { it.role == "user" && it.content == userPrompt }) }
        coVerify { messageDao.insertMessage(match { it.role == "assistant" && it.content == "Kotlin is a modern programming language." }) }
    }

    @Test
    fun `updateMessageMetadata invokes messageDao update`() = runTest {
        chatRepository.updateMessageMetadata(messageId = 5L, isKey = true, tags = "architecture")
        coVerify { messageDao.updateMessageMetadata(5L, true, "architecture") }
    }
}
