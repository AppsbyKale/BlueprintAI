package com.example.blueprintai.data

import com.example.blueprintai.ui.ArtifactExportState
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtifactExporter @Inject constructor() {

    fun createZipArchive(
        state: ArtifactExportState,
        selectedOptions: Set<String>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            if (selectedOptions.contains("Report") && state.report.isNotBlank()) {
                addZipEntry("01_Report.md", state.report, zos)
            }
            if (selectedOptions.contains("Blueprint") && state.blueprint.isNotBlank()) {
                addZipEntry("02_Blueprint_Timeline.md", state.blueprint, zos)
            }
            if (selectedOptions.contains("Concept Map") && state.conceptMap.isNotBlank()) {
                addZipEntry("03_Architecture_Concept_Map.md", state.conceptMap, zos)
            }
            if (selectedOptions.contains("Prompt") && state.prompt.isNotBlank()) {
                addZipEntry("04_Master_Prompt.md", state.prompt, zos)
            }
            if (selectedOptions.contains("Conversation") && state.conversation.isNotBlank()) {
                addZipEntry("05_Conversation_History.md", state.conversation, zos)
            }
        }
        return baos.toByteArray()
    }

    private fun addZipEntry(filename: String, content: String, zos: ZipOutputStream) {
        val entry = ZipEntry(filename)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    fun formatMergedDocument(
        state: ArtifactExportState,
        selectedOptions: Set<String>
    ): String {
        val sb = StringBuilder()
        if (selectedOptions.contains("Report") && state.report.isNotBlank()) {
            sb.append("# Report\n\n").append(state.report).append("\n\n---\n\n")
        }
        if (selectedOptions.contains("Blueprint") && state.blueprint.isNotBlank()) {
            sb.append("# Blueprint Timeline\n\n").append(state.blueprint).append("\n\n---\n\n")
        }
        if (selectedOptions.contains("Concept Map") && state.conceptMap.isNotBlank()) {
            sb.append("# Concept Map & Architecture\n\n").append(state.conceptMap).append("\n\n---\n\n")
        }
        if (selectedOptions.contains("Prompt") && state.prompt.isNotBlank()) {
            sb.append("# Master Prompt\n\n").append(state.prompt).append("\n\n---\n\n")
        }
        if (selectedOptions.contains("Conversation") && state.conversation.isNotBlank()) {
            sb.append("# Conversation History\n\n").append(state.conversation).append("\n\n")
        }
        return sb.toString().trim()
    }
}
