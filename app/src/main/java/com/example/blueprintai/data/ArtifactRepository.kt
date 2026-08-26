package com.example.blueprintai.data

import com.example.blueprintai.model.ModelManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtifactRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val modelManager: ModelManager
) {
    suspend fun generateReport(folderId: Long): String {
        val folder = folderDao.getFolderById(folderId) ?: return "Folder not found"
        val messages = messageDao.getMessagesByFolder(folderId).first()
        
        val contextText = messages.joinToString("\n") { msg ->
            val prefix = if (msg.isKeyDecision) "[KEY DECISION] " else ""
            val tags = if (msg.tags.isNotEmpty()) " (Tags: ${msg.tags})" else ""
            "${msg.role.uppercase()}: $prefix${msg.content}$tags"
        }

        val prompt = """
            Analyze the following conversation history for the project "${folder.name}".
            Prioritize messages marked as [KEY DECISION] and those with tags.
            
            Output a structured project report with the following sections:
            1. Overview/Vision
            2. Problem & Users
            3. Key Features
            4. Tech Stack & Architecture
            5. Screens & Flows
            6. Data Model
            7. Risks/Open Questions
            8. Next Steps
            
            Conversation History:
            $contextText
        """.trimIndent()

        return runGeneration(prompt)
    }

    suspend fun generateBlueprintUpdate(folderId: Long): String {
        val folder = folderDao.getFolderById(folderId) ?: return "Folder not found"
        val messages = messageDao.getMessagesByFolder(folderId).first()
        
        val contextText = messages.filter { it.isKeyDecision }.joinToString("\n") { msg ->
            "- ${msg.content}"
        }

        val prompt = """
            Based on the project "${folder.name}" and these key decisions:
            $contextText
            
            Generate a "Timeline & Decisions" entry for the current state.
            Follow the format: - **YYYY-MM-DD, HH:MM**: [Description of what was implemented/decided]
            Also provide a short "CURRENT_CONTEXT" summary (two sentences).
            
            Format exactly like this:
            ## TIMELINE & DECISIONS
            - **[DATE], [TIME]**: [Summary]
            
            ## CURRENT_CONTEXT
            [Summary]
        """.trimIndent()

        return runGeneration(prompt)
    }

    suspend fun generatePromptExport(folderId: Long): String {
        val folder = folderDao.getFolderById(folderId) ?: return "Folder not found"
        val messages = messageDao.getMessagesByFolder(folderId).first()
        
        val contextText = messages.joinToString("\n") { "${it.role.uppercase()}: ${it.content}" }

        val prompt = """
            Create a master prompt file named "${folder.name}_prompt.md" based on this brainstorming session.
            Include sections like PROJECT_SIGNATURE, SCREENS, LOGIC_TREE, DATA_SCHEMA, and PHASED IMPLEMENTATION PLAN.
            Use the conversation context to fill in the details.
            
            Conversation Context:
            $contextText
        """.trimIndent()

        return runGeneration(prompt)
    }

    private suspend fun runGeneration(prompt: String): String {
        val client = modelManager.getActiveClient()
        val response = StringBuilder()
        client.generateResponse(prompt).collect { response.append(it) }
        return response.toString()
    }
}
