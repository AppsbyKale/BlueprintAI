package com.example.blueprintai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.DebugContextInfo

@Composable
fun DebugContextDialog(
    debugInfo: DebugContextInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug: Context & Model Payload 🔍") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Mode: ${debugInfo.modelMode} • Max Capacity: ${debugInfo.maxCapacityTokens} tokens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Estimated Payload Tokens: ${debugInfo.estimatedTokensUsed} (${(debugInfo.usagePercentage * 100).toInt()}% of capacity)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "Folder Messages: ${debugInfo.totalFolderMessages} total (${debugInfo.verbatimMessagesCount} sent verbatim)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Exact Payload Sent to AI Model:", style = MaterialTheme.typography.labelLarge)
                
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp).padding(top = 8.dp)) {
                    items(debugInfo.messagesSent) { msg ->
                        Surface(
                            color = if (msg.role == "system") Color(0xFF2B2200) else if (msg.role == "user") Color(0xFF1E2638) else Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "ROLE: ${msg.role.uppercase()} (~${msg.content.length / 4} tokens)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (msg.role == "system") Color.Yellow else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SelectionContainer {
                                    Text(
                                        text = msg.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
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
