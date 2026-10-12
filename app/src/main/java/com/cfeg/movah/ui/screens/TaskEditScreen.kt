package com.cfeg.movah.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cfeg.movah.data.MoveTask
import com.cfeg.movah.engine.StorageHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskToEdit: MoveTask?,
    onBack: () -> Unit,
    onSave: (MoveTask) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(taskToEdit?.name ?: "") }
    var sourcePath by remember { mutableStateOf(taskToEdit?.sourcePath ?: "") }
    var targetPath by remember { mutableStateOf(taskToEdit?.targetPath ?: "") }
    var hour by remember { mutableIntStateOf(taskToEdit?.hour ?: 3) }
    var minute by remember { mutableIntStateOf(taskToEdit?.minute ?: 0) }
    var lifecycleEnabled by remember {
        mutableStateOf((taskToEdit?.minAgeDays ?: 0) > 0)
    }
    var minAgeDays by remember {
        mutableIntStateOf(
            if ((taskToEdit?.minAgeDays ?: 0) in 1..7) taskToEdit!!.minAgeDays else 3
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

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
                errorMessage = null
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

    fun validateAndSave() {
        if (name.isBlank()) {
            errorMessage = "Please enter a task name"
            return
        }
        if (sourcePath.isBlank()) {
            errorMessage = "Please specify a source directory"
            return
        }
        if (targetPath.isBlank()) {
            errorMessage = "Please specify a target directory"
            return
        }
        if (sourcePath.trim() == targetPath.trim()) {
            errorMessage = "Source and target directories cannot be the same"
            return
        }

        val finalMinAgeDays = if (lifecycleEnabled) minAgeDays else 0

        val task = (taskToEdit ?: MoveTask(
            name = name.trim(),
            sourcePath = sourcePath.trim(),
            targetPath = targetPath.trim(),
            hour = hour,
            minute = minute,
            minAgeDays = finalMinAgeDays
        )).copy(
            name = name.trim(),
            sourcePath = sourcePath.trim(),
            targetPath = targetPath.trim(),
            hour = hour,
            minute = minute,
            minAgeDays = finalMinAgeDays
        )
        onSave(task)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (taskToEdit == null) "New Scheduled Move" else "Edit Scheduled Move",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { validateAndSave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (taskToEdit == null) "Create Task" else "Save Changes",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Validation Error Banner
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Section 1: Task Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Task Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Task Name") },
                        placeholder = { Text("e.g. Daily Camera Backup") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Section 2: Folder Locations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Folder Locations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select source directory to move files from, and target directory to receive them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Source Path
                    OutlinedTextField(
                        value = sourcePath,
                        onValueChange = {
                            sourcePath = it
                            errorMessage = null
                        },
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
                        onValueChange = {
                            targetPath = it
                            errorMessage = null
                        },
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
                }
            }

            // Section 3: Schedule
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "$formattedTime Daily",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Executes automatically every day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { timePickerDialog.show() }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit time"
                            )
                        }
                    }
                }
            }

            // Section 4: Lifecycle Management
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lifecycle Management",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lifecycleEnabled) {
                                    "Only move files older than $minAgeDays ${if (minAgeDays == 1) "day" else "days"}"
                                } else {
                                    "Move all files regardless of age"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = lifecycleEnabled,
                            onCheckedChange = { lifecycleEnabled = it }
                        )
                    }

                    if (lifecycleEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Minimum File Age",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$minAgeDays ${if (minAgeDays == 1) "day" else "days"} old",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (minAgeDays > 1) minAgeDays-- },
                                    enabled = minAgeDays > 1
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease minimum age"
                                    )
                                }

                                Text(
                                    text = "$minAgeDays d",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                FilledTonalIconButton(
                                    onClick = { if (minAgeDays < 7) minAgeDays++ },
                                    enabled = minAgeDays < 7
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase minimum age"
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Files created or modified within the last $minAgeDays ${if (minAgeDays == 1) "day" else "days"} will stay in the source directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
