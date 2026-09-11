package com.example.blueprintai.ui

import com.example.blueprintai.R
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.blueprintai.data.DebugContextInfo
import com.example.blueprintai.data.Message

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingResponse by viewModel.currentStreamingResponse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val conceptExplanation by viewModel.conceptExplanation.collectAsState()
    val isExplainingConcepts by viewModel.isExplainingConcepts.collectAsState()
    val currentDebugInfo by viewModel.currentDebugInfo.collectAsState()
    
    var selectedDebugInfo by remember { mutableStateOf<DebugContextInfo?>(null) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
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

    val lastItemIndex = remember(messages.size, streamingResponse) {
        val totalCount = messages.size + if (streamingResponse.isNotEmpty()) 1 else 0
        (totalCount - 1).coerceAtLeast(0)
    }

    // Scroll to bottom when opening the app or switching folders
    LaunchedEffect(currentFolderId, messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(lastItemIndex)
        }
    }

    // Smooth scroll to bottom when new messages or streaming tokens arrive
    LaunchedEffect(messages.size, streamingResponse) {
        if (isAtBottom && (messages.isNotEmpty() || streamingResponse.isNotEmpty())) {
            listState.animateScrollToItem(lastItemIndex)
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

        currentDebugInfo?.let { debug ->
            Surface(
                color = Color(0xFF141414),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Context: ${debug.estimatedTokensUsed} / ${debug.maxCapacityTokens} tokens (${(debug.usagePercentage * 100).toInt()}% used)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = if (debug.isSummaryIncluded) "20 verbatim + Summary" else "${debug.totalFolderMessages} messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { debug.usagePercentage },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = when {
                            debug.usagePercentage > 0.9f -> MaterialTheme.colorScheme.error
                            debug.usagePercentage > 0.7f -> Color.Yellow
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = Color(0xFF222222)
                    )
                }
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
                        onUpdateTags = { viewModel.updateTags(message, it) },
                        onExplainConcepts = { viewModel.explainConcepts(message) },
                        onInspectDebug = {
                            selectedDebugInfo = viewModel.getDebugInfoForMessage(message.id)
                        }
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
                keyboardController?.hide()
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

    if (conceptExplanation != null || isExplainingConcepts) {
        ConceptExplanationDialog(
            explanation = conceptExplanation,
            isLoading = isExplainingConcepts,
            onDismiss = { viewModel.clearConceptExplanation() }
        )
    }

    selectedDebugInfo?.let { debugInfo ->
        DebugContextDialog(
            debugInfo = debugInfo,
            onDismiss = { selectedDebugInfo = null }
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
                            labelColor = Color.LightGray
                        ),
                        border = null
                    )
                }
            }
        }
    }
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_attach))
            }
            IconButton(
                onClick = onMicClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isListening) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = stringResource(R.string.cd_voice)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.placeholder_ask_blueprint), color = Color.Gray) },
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
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.cd_send))
            }
        }
    }
}
