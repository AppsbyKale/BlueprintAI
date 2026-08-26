package com.example.blueprintai.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.blueprintai.data.Message

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingResponse by viewModel.currentStreamingResponse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
    }

    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            inputText = recognizedText
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(it) ?: "application/octet-stream"
                var name = "unknown"
                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
                viewModel.addAttachment(it, type, name)
            }
        }
    )

    LaunchedEffect(messages.size, streamingResponse) {
        if (isAtBottom && (messages.isNotEmpty() || streamingResponse.isNotEmpty())) {
            listState.animateScrollToItem((messages.size + if (streamingResponse.isNotEmpty()) 1 else 0).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (attachments.isNotEmpty()) {
            AttachmentNoticeBar(attachments)
        }
        
        if (isGenerating) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
        
        if (isListening) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Listening...",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onToggleStar = { viewModel.toggleKeyDecision(message) },
                        onUpdateTags = { viewModel.updateTags(message, it) }
                    )
                }
                if (streamingResponse.isNotEmpty()) {
                    item {
                        StreamingBubble(streamingResponse)
                    }
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            },
            onAttach = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onMicClick = {
                if (isListening) viewModel.stopListening()
                else viewModel.startListening()
            },
            isListening = isListening,
            enabled = !isGenerating && inputText.isNotBlank()
        )
    }
}

@Composable
fun AttachmentNoticeBar(attachments: List<com.example.blueprintai.data.Attachment>) {
    Surface(
        color = Color(0xFF1A1A1A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Attachments",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(attachments) { attachment ->
                    SuggestionChip(
                        onClick = { },
                        label = { 
                            Text(
                                attachment.aiSuggestedName ?: attachment.originalName,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF333333),
                            labelColor = Color.White
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onToggleStar: () -> Unit,
    onUpdateTags: (String) -> Unit
) {
    val isUser = message.role == "user"
    var showTagDialog by remember { mutableStateOf(false) }

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
                            onLongPress = { showTagDialog = true }
                        )
                    }
            ) {
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
            Text(
                text = content,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
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

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    enabled: Boolean
) {
    Surface(
        color = Color(0xFF111111),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.LightGray)) {
                Icon(Icons.Default.Add, contentDescription = "Attach")
            }
            IconButton(
                onClick = onMicClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isListening) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice"
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask BlueprintAI...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF222222),
                    unfocusedContainerColor = Color(0xFF222222),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
