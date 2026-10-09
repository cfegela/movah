package com.cfeg.movah.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfeg.movah.data.MoveLog
import com.cfeg.movah.data.MoveTask
import com.cfeg.movah.data.MovahDatabase
import com.cfeg.movah.scheduler.AlarmScheduler
import com.cfeg.movah.scheduler.FileMoverService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MovahDatabase.getDatabase(application).moveDao()

    val tasks: StateFlow<List<MoveTask>> = dao.getAllTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<MoveLog>> = dao.getAllLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTask(task: MoveTask) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (task.id == 0L) {
                val newId = dao.insertTask(task)
                val insertedTask = task.copy(id = newId)
                if (insertedTask.isEnabled) {
                    AlarmScheduler.scheduleTask(context, insertedTask)
                }
            } else {
                dao.updateTask(task)
                if (task.isEnabled) {
                    AlarmScheduler.scheduleTask(context, task)
                } else {
                    AlarmScheduler.cancelTask(context, task.id)
                }
            }
        }
    }

    fun deleteTask(task: MoveTask) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            AlarmScheduler.cancelTask(context, task.id)
            dao.deleteTask(task)
        }
    }

    fun toggleTask(task: MoveTask, enabled: Boolean) {
        viewModelScope.launch {
            val updated = task.copy(isEnabled = enabled)
            dao.updateTask(updated)
            val context = getApplication<Application>()
            if (enabled) {
                AlarmScheduler.scheduleTask(context, updated)
            } else {
                AlarmScheduler.cancelTask(context, updated.id)
            }
        }
    }

    fun runTaskNow(task: MoveTask) {
        FileMoverService.startRunNow(getApplication(), task.id)
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAllLogs()
        }
    }
}
