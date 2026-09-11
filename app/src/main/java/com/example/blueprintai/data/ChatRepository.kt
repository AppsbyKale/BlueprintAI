package com.example.blueprintai.data

import com.example.blueprintai.domain.repository.IChatRepository
import com.example.blueprintai.model.ChatMessage
import com.example.blueprintai.model.ModelManager
import com.example.blueprintai.model.ToolInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val settingsDao: SettingsDao,
    private val modelManager: ModelManager,
    private val toolInterceptor: ToolInterceptor
) : IChatRepository {
    // Cache for older conversation summaries per folder: folderId -> Pair(Pair(olderMsgCount, starredCount), summaryText)
    private val folderSummaries = ConcurrentHashMap<Long, Pair<Pair<Int, Int>, String>>()

    // Debug context info tracking
    private val _currentDebugInfo = MutableStateFlow<DebugContextInfo?>(null)
    override val currentDebugInfo: StateFlow<DebugContextInfo?> = _currentDebugInfo.asStateFlow()

    private val messageDebugMap = ConcurrentHashMap<Long, DebugContextInfo>()

    override fun getDebugInfoForMessage(messageId: Long): DebugContextInfo? = messageDebugMap[messageId]

    override fun getMessages(folderId: Long): Flow<List<Message>> = messageDao.getMessagesByFolder(folderId)

    override fun searchMessages(query: String): Flow<List<Message>> = messageDao.searchMessages(query)

    override fun searchMessagesInFolder(folderId: Long, query: String): Flow<List<Message>> = 
        messageDao.searchMessagesInFolder(folderId, query)

    override suspend fun sendMessage(folderId: Long, content: String): Flow<String> = flow {
        // Save user message
        val userMessage = Message(folderId = folderId, content = content, role = "user")
        messageDao.insertMessage(userMessage)

        // Get active model client (Local, Desktop, or Gemini)
        val client = modelManager.getActiveClient()
        val capacity = client.getContextCapacity()
        val activeSettings = settingsDao.getSettings().first() ?: Settings()

        // Load all stored messages for this folder
        val allMessages = messageDao.getMessagesByFolder(folderId).first()

        // Build efficient chat payload using 20-message sliding window + detailed project & starred summary for older history
        var currentHistory = buildCompressedChatHistory(folderId, allMessages, capacity)

        val totalChars = currentHistory.sumOf { it.content.length }
        val estTokens = (totalChars / 4).coerceAtLeast(1)

        val debugInfo = DebugContextInfo(
            modelMode = activeSettings.modelMode,
            maxCapacityTokens = capacity,
            estimatedTokensUsed = estTokens,
            totalFolderMessages = allMessages.size,
            verbatimMessagesCount = if (allMessages.size > 20) 20 else allMessages.size,
            isSummaryIncluded = allMessages.size > 20,
            messagesSent = currentHistory
        )
        _currentDebugInfo.value = debugInfo

        var loop = true
        var interactionCount = 0

        while (loop && interactionCount < 5) {
            val fullResponse = StringBuilder()
            client.generateChatResponse(currentHistory).collect { chunk ->
                fullResponse.append(chunk)
                emit(chunk)
            }

            val responseText = fullResponse.toString().trim()
            if (responseText.startsWith("Error", ignoreCase = true)) {
                // Do not save error messages to DB as valid assistant turns
                loop = false
            } else if (responseText.startsWith("{") && responseText.endsWith("}")) {
                // Potential tool call
                val toolResult = toolInterceptor.intercept(responseText)
                emit("\n[Tool Result: $toolResult]\n")
                currentHistory = currentHistory + ChatMessage(role = "assistant", content = responseText) + ChatMessage(role = "user", content = "Tool result: $toolResult")
                interactionCount++
            } else {
                // Normal response
                val aiMessage = Message(folderId = folderId, content = responseText, role = "assistant")
                val insertedId = messageDao.insertMessage(aiMessage)
                messageDebugMap[insertedId] = debugInfo
                loop = false
            }
        }
    }

    private suspend fun buildCompressedChatHistory(folderId: Long, allMessages: List<Message>, maxCapacity: Int): List<ChatMessage> {
        val cleanMessages = allMessages.filter { !it.content.trim().startsWith("Error", ignoreCase = true) }
        val windowSize = 20
        val compressionThresholdTokens = (maxCapacity * 0.60f).toInt().coerceAtLeast(1200)
        val totalRawTokens = cleanMessages.sumOf { (it.content.length / 4).coerceAtLeast(1) }

        if (cleanMessages.size <= windowSize && totalRawTokens <= compressionThresholdTokens) {
            // Under 20 messages & under 60% capacity threshold: send all verbatim
            return cleanMessages.map { ChatMessage(role = it.role, content = it.content) }
        }

        // Over 20 messages or over 60% capacity threshold: split into older history and recent verbatim messages
        val olderMessages = cleanMessages.dropLast(windowSize)
        val recentMessages = cleanMessages.takeLast(windowSize)
        val starredMessages = cleanMessages.filter { it.isKeyDecision }

        val cacheKey = Pair(olderMessages.size, starredMessages.size)
        val cachedSummary = folderSummaries[folderId]

        val summaryText = if (cachedSummary != null && cachedSummary.first == cacheKey) {
            cachedSummary.second
        } else {
            val newSummary = generateDetailedProjectSummary(olderMessages, starredMessages)
            folderSummaries[folderId] = Pair(cacheKey, newSummary)
            newSummary
        }

        val systemSummaryMessage = ChatMessage(
            role = "system",
            content = "APP BRAINSTORMING CONTEXT & KEY DECISIONS (Auto-compressed prior history):\n$summaryText"
        )

        return listOf(systemSummaryMessage) + recentMessages.map { ChatMessage(role = it.role, content = it.content) }
    }

    private suspend fun generateDetailedProjectSummary(olderMessages: List<Message>, starredMessages: List<Message>): String {
        return try {
            val client = modelManager.getActiveClient()
            val olderContentText = olderMessages.joinToString("\n") { "${it.role.uppercase()}: ${it.content}" }
            val starredText = if (starredMessages.isNotEmpty()) {
                starredMessages.joinToString("\n") { "- [STARRED] (${it.role.uppercase()}): ${it.content}" }
            } else "None marked yet."

            val prompt = """
                Analyze the following app brainstorming conversation history and output a structured summary with 3 sections:

                1. APP CONCEPT & VISION (2 sentences describing the app being brainstormed)
                2. KEY FEATURES & REQUIREMENTS (Bullet points of key features agreed upon)
                3. KEY DECISIONS & STARRED HIGHLIGHTS (Summarize the important decisions from these starred entries):
                $starredText

                Prior Conversation Context:
                $olderContentText
            """.trimIndent()

            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            response.toString().trim().takeIf { it.isNotEmpty() }
                ?: "App Brainstorming Session in progress."
        } catch (e: Exception) {
            "App Brainstorming Session in progress."
        }
    }

    override suspend fun updateMessageMetadata(messageId: Long, isKey: Boolean, tags: String) {
        messageDao.updateMessageMetadata(messageId, isKey, tags)
    }

    override suspend fun explainConcepts(messageContent: String): String {
        val prompt = """
            Analyze the following text/code snippet and explain it for a beginner software builder (a visual learner).
            
            Format your response clearly into 3 distinct sections:
            
            1. KEY CONCEPTS & TERMS
            (Define 2-4 key technical terms or keywords mentioned in plain English with simple analogies).
            
            2. RELATIONSHIPS & CAUSE-AND-EFFECT
            (Explain how the components interact. E.g., "If you change X, it affects Y").
            
            3. VISUAL FLOW / DIAGRAM
            (Use simple text/ASCII boxes or step-by-step arrows to show the flow of data or execution).
            
            Snippet to Explain:
            $messageContent
        """.trimIndent()

        // 1. Try active primary model client (Desktop / Auto / Phone)
        try {
            val client = modelManager.getActiveClient()
            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            val text = response.toString().trim()
            if (text.isNotEmpty() && !text.startsWith("Error:")) {
                return text
            }
        } catch (e: Throwable) {
            // Primary failed, proceed to local fallback
        }

        // 2. Fallback to local phone model or Gemini API
        return try {
            val fallbackClient = modelManager.getLocalFallbackClient()
            val response = StringBuilder()
            fallbackClient.generateResponse(prompt).collect { response.append(it) }
            val text = response.toString().trim()
            if (text.isNotEmpty()) text else "Unable to generate concept explanation."
        } catch (e: Throwable) {
            "Unable to generate concept explanation: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    override fun getAttachmentsForMessage(messageId: Long): Flow<List<Attachment>> = 
        attachmentDao.getAttachmentsForMessage(messageId)

    override fun getAttachmentsForFolder(folderId: Long): Flow<List<Attachment>> = 
        attachmentDao.getAttachmentsForFolder(folderId)

    override suspend fun addAttachment(attachment: Attachment) {
        attachmentDao.insertAttachment(attachment)
    }

    override suspend fun generateSuggestedName(originalName: String, extractedText: String?): String {
        return try {
            val client = modelManager.getActiveClient()
            val prompt = """
                Suggest a very short, descriptive filename (max 5 words) for an attachment.
                Original name: $originalName
                Extracted text snippet: ${extractedText?.take(300) ?: "None"}
                Return ONLY the suggested name (slug-style or Title Case), no quotes or explanation.
            """.trimIndent()
            
            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            response.toString().trim().takeIf { it.isNotEmpty() } ?: "Analyzed_$originalName"
        } catch (e: Exception) {
            "Analyzed_$originalName"
        }
    }
}
