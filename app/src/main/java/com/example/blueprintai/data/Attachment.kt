package com.example.blueprintai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val messageId: Long? = null,
    val originalName: String,
    val aiSuggestedName: String? = null,
    val type: String, // "image", "document", etc.
    val extractedText: String? = null,
    val uri: String,
    val timestamp: Long = System.currentTimeMillis()
)
