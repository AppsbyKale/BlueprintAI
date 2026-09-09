package com.example.blueprintai.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.Message

@Composable
fun MessageBubble(
    message: Message,
    onToggleStar: () -> Unit,
    onUpdateTags: (String) -> Unit,
    onExplainConcepts: () -> Unit,
    onInspectDebug: () -> Unit
) {
    val isUser = message.role == "user"
    var showTagDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isUser) Color(0xFF222222) else Color(0xFF111111),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true }
                        )
                    }
            ) {
                SelectionContainer {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val isError = message.content.startsWith("Error:")
                        Text(
                            text = message.content,
                            color = if (isError) MaterialTheme.colorScheme.error else Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (message.tags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                message.tags.split(",").forEach { tag ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(tag.trim(), style = MaterialTheme.typography.labelSmall) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFF333333),
                                            labelColor = Color.LightGray
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        showMenu = false
                    }
                )
                if (!isUser) {
                    DropdownMenuItem(
                        text = { Text("Explain Concepts & Relationships") },
                        onClick = {
                            showMenu = false
                            onExplainConcepts()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Debug: Inspect Context & Tokens") },
                        onClick = {
                            showMenu = false
                            onInspectDebug()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Update Tags") },
                    onClick = {
                        showMenu = false
                        showTagDialog = true
                    }
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                IconButton(onClick = onToggleStar, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (message.isKeyDecision) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star",
                        tint = if (message.isKeyDecision) Color.Yellow else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (message.isKeyDecision) {
                    Text(
                        "Key Decision",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Yellow,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }

    if (showTagDialog) {
        TagDialog(
            initialTags = message.tags,
            onDismiss = { showTagDialog = false },
            onConfirm = { 
                onUpdateTags(it)
                showTagDialog = false
            }
        )
    }
}

@Composable
fun StreamingBubble(content: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            SelectionContainer {
                Text(
                    text = content,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun TagDialog(
    initialTags: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var tags by remember { mutableStateOf(initialTags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Tags") },
        text = {
            TextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Comma separated tags") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tags) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
