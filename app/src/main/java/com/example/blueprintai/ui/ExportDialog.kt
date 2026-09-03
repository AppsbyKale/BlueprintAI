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
    onExport: (Set<String>, String) -> Unit
) {
    var selectedArtifacts by remember { mutableStateOf(setOf<String>()) }
    var selectedFormat by remember { mutableStateOf("md") }

    val options = listOf("Report", "Blueprint", "Concept Map", "Prompt", "Conversation")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Share") },
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
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("md", "txt", "zip").forEach { format ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format }
                                )
                                Text(format)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(selectedArtifacts, selectedFormat) },
                enabled = !state.isGenerating && selectedArtifacts.isNotEmpty()
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
