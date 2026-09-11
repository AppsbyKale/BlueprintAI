package com.example.blueprintai.domain.model

enum class IpMode(val key: String) {
    LOCAL("LOCAL"),
    PUBLIC("PUBLIC");

    companion object {
        fun fromKey(key: String): IpMode = when (key) {
            "PUBLIC" -> PUBLIC
            else -> LOCAL
        }
    }
}
