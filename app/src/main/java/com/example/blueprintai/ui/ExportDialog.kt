package com.example.blueprintai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(
    state: ArtifactExportState,
    onDismiss: () -> Unit,
    onDownloadFile: (Set<String>, String) -> Unit,
    onShareText: (Set<String>, String) -> Unit = { _, _ -> }
) {
    var selectedArtifacts by remember { mutableStateOf(setOf("Report", "Concept Map", "Prompt")) }
    var selectedFormat by remember { mutableStateOf("zip") }

    val options = listOf("Report", "Blueprint", "Concept Map", "Prompt", "Conversation")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Download Artifacts") },
        text = {
            if (state.isGenerating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Generating artifacts...")
                }
            } else {
                Column {
                    Text(
                        "Select artifacts to export:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedArtifacts.contains(option),
                                onCheckedChange = { 
                                    selectedArtifacts = if (it) selectedArtifacts + option else selectedArtifacts - option
                                }
                            )
                            Text(option)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export Format", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("zip", "md", "txt").forEach { format ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format }
                                )
                                Text(format.uppercase())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onShareText(selectedArtifacts, selectedFormat) },
                    enabled = !state.isGenerating && selectedArtifacts.isNotEmpty()
                ) {
                    Text("Share Text")
                }
                Button(
                    onClick = { onDownloadFile(selectedArtifacts, selectedFormat) },
                    enabled = !state.isGenerating && selectedArtifacts.isNotEmpty()
                ) {
                    Text("Download File")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
