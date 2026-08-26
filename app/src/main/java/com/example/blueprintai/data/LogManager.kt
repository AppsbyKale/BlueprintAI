package com.example.blueprintai.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DiagnosticLog(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, ERROR, DEBUG
    val tag: String,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

@Singleton
class LogManager @Inject constructor() {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    fun log(level: String, tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        val newLog = DiagnosticLog(level = level, tag = tag, message = message, metadata = metadata)
        _logs.value = (_logs.value + newLog).takeLast(1000)
    }

    fun exportTrainingData(): String {
        val json = Json { prettyPrint = false }
        return _logs.value.joinToString("\n") { log ->
            json.encodeToString(DiagnosticLog.serializer(), log)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
