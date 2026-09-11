package com.example.blueprintai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.ui.cleanDesktopUrl
import kotlinx.coroutines.launch

@Composable
fun RemoteProfileDialog(
    initialProfile: RemoteModelProfile? = null,
    onDismiss: () -> Unit,
    onConfirm: (label: String, localIp: String, publicIp: String, modelName: String, apiKey: String) -> Unit,
    onFetchModels: suspend (url: String, apiKey: String) -> List<String> = { _, _ -> emptyList() }
) {
    var label by remember(initialProfile) { mutableStateOf(initialProfile?.label ?: "") }
    var localIp by remember(initialProfile) { mutableStateOf(initialProfile?.localIpUrl ?: "") }
    var publicIp by remember(initialProfile) { mutableStateOf(initialProfile?.publicIpUrl ?: "") }
    var modelName by remember(initialProfile) { mutableStateOf(initialProfile?.modelName ?: "") }
    var apiKey by remember(initialProfile) { mutableStateOf(initialProfile?.apiKey ?: "") }

    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile == null) "Add Remote Model Profile" else "Edit Remote Model Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g. Home Desktop (LM Studio)") },
                    label = { Text("Profile Name / Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = localIp,
                    onValueChange = { localIp = it },
                    placeholder = { Text("e.g. 192.168.1.50:1234") },
                    label = { Text("Local IP / Home Wi-Fi URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = publicIp,
                    onValueChange = { publicIp = it },
                    placeholder = { Text("e.g. 73.120.10.5:1234 or LM Link") },
                    label = { Text("Public IP / Away URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            placeholder = { Text("e.g. gemma-2-9b-it or local-model") },
                            label = { Text("Target Model ID (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (availableModels.isNotEmpty()) {
                                    IconButton(onClick = { modelDropdownExpanded = !modelDropdownExpanded }) {
                                        Text("▼", color = Color.Gray)
                                    }
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        modelName = model
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isFetchingModels = true
                                val target = cleanDesktopUrl(localIp.ifBlank { publicIp })
                                if (target.isNotBlank()) {
                                    val fetched = onFetchModels(target, apiKey)
                                    if (fetched.isNotEmpty()) {
                                        availableModels = fetched
                                        modelDropdownExpanded = true
                                        if (modelName.isBlank()) {
                                            modelName = fetched.first()
                                        }
                                    }
                                }
                                isFetchingModels = false
                            }
                        },
                        enabled = !isFetchingModels && (localIp.isNotBlank() || publicIp.isNotBlank())
                    ) {
                        Text(if (isFetchingModels) "Fetching models..." else "Fetch Models from Server")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        label.ifBlank { "Desktop Server" },
                        localIp.trim(),
                        publicIp.trim(),
                        modelName.trim(),
                        apiKey.trim()
                    )
                },
                enabled = localIp.isNotBlank() || publicIp.isNotBlank()
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
