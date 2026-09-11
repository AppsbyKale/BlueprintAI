package com.example.blueprintai.data

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    private val encryptedPreferences: SharedPreferences
) {
    fun getString(key: String, defaultValue: String = ""): String {
        return encryptedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putString(key: String, value: String) {
        encryptedPreferences.edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        encryptedPreferences.edit().remove(key).apply()
    }

    fun clear() {
        encryptedPreferences.edit().clear().apply()
    }

    companion object {
        const val KEY_GEMINI_API_KEY = "secure_gemini_api_key"
        fun profileApiKey(profileId: Long) = "secure_profile_api_key_$profileId"
    }
}
