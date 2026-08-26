package com.example.blueprintai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val modelMode: String = "Auto", // "Auto", "Desktop", "Phone"
    val localModelPath: String = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm",
    val desktopUrl: String = "http://192.168.1.10:1234/v1",
    val isTtsEnabled: Boolean = false,
    val geminiApiKey: String = ""
)
