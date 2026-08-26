package com.example.blueprintai.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.Folder

@Composable
fun SidebarContent(
    viewModel: ChatViewModel,
    onFolderClick: (Long) -> Unit,
    onExportClick: (Folder) -> Unit
) {
    val folders by viewModel.folders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var folderToMerge by remember { mutableStateOf<Folder?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search folders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF111111),
                unfocusedContainerColor = Color(0xFF111111),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { showNewFolderDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF222222),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Folder")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "FOLDERS", 
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        LazyColumn {
            items(folders) { folder ->
                var showContextMenu by remember { mutableStateOf(false) }
                
                Box {
                    NavigationDrawerItem(
                        label = { Text(folder.name) },
                        selected = currentFolderId == folder.id,
                        onClick = { onFolderClick(folder.id) },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { showContextMenu = true },
                                    onTap = { onFolderClick(folder.id) }
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF333333),
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.LightGray
                        )
                    )
                    
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Name") },
                            onClick = {
                                folderToEdit = folder
                                showContextMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = {
                                onExportClick(folder)
                                showContextMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Merge with...") },
                            onClick = {
                                folderToMerge = folder
                                showContextMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = {
                                viewModel.deleteFolder(folder)
                                showContextMenu = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        FolderDialog(
            title = "New Folder",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showNewFolderDialog = false
            }
        )
    }

    folderToEdit?.let { folder ->
        FolderDialog(
            title = "Edit Folder Name",
            initialName = folder.name,
            onDismiss = { folderToEdit = null },
            onConfirm = { name ->
                viewModel.updateFolder(folder.id, name)
                folderToEdit = null
            }
        )
    }

    folderToMerge?.let { source ->
        MergeFolderDialog(
            folders = folders.filter { it.id != source.id },
            onDismiss = { folderToMerge = null },
            onConfirm = { targetId ->
                viewModel.mergeFolders(source.id, targetId)
                folderToMerge = null
            }
        )
    }
}

@Composable
fun MergeFolderDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Folder") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(folders) { folder ->
                    TextButton(
                        onClick = { onConfirm(folder.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(folder.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FolderDialog(
    title: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Folder Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
