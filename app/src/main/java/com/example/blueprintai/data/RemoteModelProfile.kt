package com.example.blueprintai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_model_profiles")
data class RemoteModelProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,                             // e.g., "Home Desktop (LM Studio)", "Office Workstation"
    val localIpUrl: String,                        // e.g., "http://192.168.1.50:1234/v1"
    val publicIpUrl: String = "",                  // e.g., "http://73.120.10.5:1234/v1" or LM Link
    val apiKey: String = "",                       // Optional API Key
    val isActive: Boolean = false,                 // Selected active server profile
    val createdAt: Long = System.currentTimeMillis()
)
