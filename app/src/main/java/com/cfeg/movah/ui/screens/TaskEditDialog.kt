package com.cfeg.movah.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cfeg.movah.data.MoveTask
import com.cfeg.movah.engine.StorageHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    taskToEdit: MoveTask?,
    onDismiss: () -> Unit,
    onSave: (MoveTask) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(taskToEdit?.name ?: "") }
    var sourcePath by remember { mutableStateOf(taskToEdit?.sourcePath ?: "") }
    var targetPath by remember { mutableStateOf(taskToEdit?.targetPath ?: "") }
    var hour by remember { mutableIntStateOf(taskToEdit?.hour ?: 3) }
    var minute by remember { mutableIntStateOf(taskToEdit?.minute ?: 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Folder pickers
    var selectingTargetForSource by remember { mutableStateOf(true) }
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = StorageHelper.resolvePathFromTreeUri(uri)
            if (resolved != null) {
                if (selectingTargetForSource) {
                    sourcePath = resolved
                } else {
                    targetPath = resolved
                }
            }
        }
    }

    // Time Picker Dialog
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
            },
            hour,
            minute,
            false // 12-hour AM/PM format
        )
    }

    val formattedTime = remember(hour, minute) {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (taskToEdit == null) "Add Scheduled Move" else "Edit Scheduled Move")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    placeholder = { Text("e.g. Daily Camera Backup") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Source Path
                OutlinedTextField(
                    value = sourcePath,
                    onValueChange = { sourcePath = it },
                    label = { Text("Source Directory") },
                    placeholder = { Text("/storage/emulated/0/...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            selectingTargetForSource = true
                            folderPicker.launch(null)
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Pick source folder")
                        }
                    }
                )

                // Target Path
                OutlinedTextField(
                    value = targetPath,
                    onValueChange = { targetPath = it },
                    label = { Text("Target Directory") },
                    placeholder = { Text("/storage/emulated/0/...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            selectingTargetForSource = false
                            folderPicker.launch(null)
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Pick target folder")
                        }
                    }
                )

                // Scheduled Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Execution Time", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$formattedTime (Daily)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { timePickerDialog.show() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit time")
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a task name"
                        return@Button
                    }
                    if (sourcePath.isBlank()) {
                        errorMessage = "Please specify a source directory"
                        return@Button
                    }
                    if (targetPath.isBlank()) {
                        errorMessage = "Please specify a target directory"
                        return@Button
                    }
                    if (sourcePath.trim() == targetPath.trim()) {
                        errorMessage = "Source and target directories cannot be the same"
                        return@Button
                    }

                    val task = (taskToEdit ?: MoveTask(
                        name = name.trim(),
                        sourcePath = sourcePath.trim(),
                        targetPath = targetPath.trim(),
                        hour = hour,
                        minute = minute
                    )).copy(
                        name = name.trim(),
                        sourcePath = sourcePath.trim(),
                        targetPath = targetPath.trim(),
                        hour = hour,
                        minute = minute
                    )
                    onSave(task)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
