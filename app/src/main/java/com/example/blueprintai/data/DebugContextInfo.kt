package com.example.blueprintai.data

import com.example.blueprintai.model.ChatMessage

data class DebugContextInfo(
    val modelMode: String = "Auto",
    val maxCapacityTokens: Int = 4096,
    val estimatedTokensUsed: Int = 0,
    val totalFolderMessages: Int = 0,
    val verbatimMessagesCount: Int = 0,
    val isSummaryIncluded: Boolean = false,
    val messagesSent: List<ChatMessage> = emptyList()
) {
    val usagePercentage: Float
        get() = if (maxCapacityTokens > 0) (estimatedTokensUsed.toFloat() / maxCapacityTokens.toFloat()).coerceIn(0f, 1f) else 0f
}
