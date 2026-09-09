package com.example.blueprintai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun cleanDialogText(input: String?): String {
    if (input.isNullOrBlank()) return "No explanation available."
    return input
        .replace(Regex("\\*\\*([^*]+)\\*\\*")) { it.groupValues[1] }
        .replace(Regex("\\*([^*]+)\\*")) { it.groupValues[1] }
        .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF]"), "")
        .trim()
}

@Composable
fun ConceptExplanationDialog(
    explanation: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Concept Breakdown & Relationships") },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyzing concepts & visual flows...")
                }
            } else {
                SelectionContainer {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            Text(
                                text = cleanDialogText(explanation),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
