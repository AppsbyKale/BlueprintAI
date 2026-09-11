package com.example.blueprintai.testing.unit

import com.example.blueprintai.data.ArtifactExporter
import com.example.blueprintai.ui.ArtifactExportState
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ArtifactExporterTest {

    private val exporter = ArtifactExporter()

    @Test
    fun `createZipArchive bundles selected artifacts into zip entries`() {
        val state = ArtifactExportState(
            report = "Project Vision and Requirements",
            conceptMap = "UI -> ViewModel -> Room Diagram",
            prompt = "Master Prompt Content"
        )
        val selectedOptions = setOf("Report", "Concept Map", "Prompt")

        val zipBytes = exporter.createZipArchive(state, selectedOptions)
        assertTrue(zipBytes.isNotEmpty())

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals(3, entryNames.size)
        assertTrue(entryNames.contains("01_Report.md"))
        assertTrue(entryNames.contains("03_Architecture_Concept_Map.md"))
        assertTrue(entryNames.contains("04_Master_Prompt.md"))
    }

    @Test
    fun `formatMergedDocument builds markdown text from selected options`() {
        val state = ArtifactExportState(
            report = "Report Content",
            blueprint = "Timeline Content"
        )
        val selectedOptions = setOf("Report", "Blueprint")

        val result = exporter.formatMergedDocument(state, selectedOptions)

        assertTrue(result.contains("# Report"))
        assertTrue(result.contains("Report Content"))
        assertTrue(result.contains("# Blueprint Timeline"))
        assertTrue(result.contains("Timeline Content"))
    }
}
