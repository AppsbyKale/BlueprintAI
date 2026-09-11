package com.example.blueprintai.domain.model

enum class ModelMode(val key: String) {
    AUTO("Auto"),
    REMOTE("Remote"),
    LOCAL("Local");

    companion object {
        fun fromKey(key: String): ModelMode = when (key) {
            "Desktop", "Remote" -> REMOTE
            "Phone", "Local" -> LOCAL
            else -> AUTO
        }
    }
}
