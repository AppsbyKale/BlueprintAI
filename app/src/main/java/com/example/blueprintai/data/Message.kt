package com.example.blueprintai.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val content: String,
    val role: String, // "user", "assistant", "system"
    val isKeyDecision: Boolean = false,
    val tags: String = "", // Comma-separated
    val timestamp: Long = System.currentTimeMillis()
)

@Fts4(contentEntity = Message::class)
@Entity(tableName = "messages_fts")
data class MessageFts(
    val content: String
)
