package com.cfeg.movah.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileMoverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            AlarmScheduler.ACTION_EXECUTE_MOVE -> {
                val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val serviceIntent = Intent(context, FileMoverService::class.java).apply {
                        this.action = FileMoverService.ACTION_RUN_TASK
                        putExtra(FileMoverService.EXTRA_TASK_ID, taskId)
                        putExtra(FileMoverService.EXTRA_IS_MANUAL, false)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmScheduler.rescheduleAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
