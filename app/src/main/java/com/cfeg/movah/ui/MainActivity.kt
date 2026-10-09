package com.cfeg.movah.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cfeg.movah.data.MoveTask
import com.cfeg.movah.engine.StorageHelper
import com.cfeg.movah.ui.screens.LogsScreen
import com.cfeg.movah.ui.screens.TaskEditDialog
import com.cfeg.movah.ui.screens.TasksScreen
import com.cfeg.movah.ui.theme.MovahTheme
import com.cfeg.movah.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MovahTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var showEditDialog by remember { mutableStateOf(false) }
                var taskToEdit by remember { mutableStateOf<MoveTask?>(null) }
                var hasStoragePermission by remember {
                    mutableStateOf(StorageHelper.hasManageStoragePermission(this))
                }

                // Check permission on resume (e.g. returning from system Settings)
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasStoragePermission = StorageHelper.hasManageStoragePermission(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Request POST_NOTIFICATIONS for Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* granted or denied */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val tasks by viewModel.tasks.collectAsState()
                val logs by viewModel.logs.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (selectedTab == 0) "Scheduled Moves" else "Activity Logs",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedules") },
                                label = { Text("Schedules") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                                label = { Text("Logs") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            0 -> TasksScreen(
                                tasks = tasks,
                                hasStoragePermission = hasStoragePermission,
                                onGrantStoragePermission = {
                                    StorageHelper.openManageStorageSettings(this@MainActivity)
                                },
                                onAddTask = {
                                    taskToEdit = null
                                    showEditDialog = true
                                },
                                onEditTask = { task ->
                                    taskToEdit = task
                                    showEditDialog = true
                                },
                                onDeleteTask = { task ->
                                    viewModel.deleteTask(task)
                                },
                                onToggleTask = { task, enabled ->
                                    viewModel.toggleTask(task, enabled)
                                },
                                onRunTaskNow = { task ->
                                    viewModel.runTaskNow(task)
                                }
                            )

                            1 -> LogsScreen(
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                        }
                    }

                    if (showEditDialog) {
                        TaskEditDialog(
                            taskToEdit = taskToEdit,
                            onDismiss = {
                                showEditDialog = false
                                taskToEdit = null
                            },
                            onSave = { task ->
                                viewModel.saveTask(task)
                                showEditDialog = false
                                taskToEdit = null
                            }
                        )
                    }
                }
            }
        }
    }
}
