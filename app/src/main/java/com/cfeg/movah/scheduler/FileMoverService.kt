package com.cfeg.movah.scheduler

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.cfeg.movah.MovahApp
import com.cfeg.movah.data.MoveLog
import com.cfeg.movah.data.MovahDatabase
import com.cfeg.movah.engine.FileMoverEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FileMoverService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_RUN_TASK = "com.cfeg.movah.ACTION_RUN_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_IS_MANUAL = "extra_is_manual"
        private const val NOTIFICATION_ID = 1001

        fun startRunNow(context: Context, taskId: Long) {
            val intent = Intent(context, FileMoverService::class.java).apply {
                action = ACTION_RUN_TASK
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_IS_MANUAL, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "movah:transfer_wakelock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        val isManual = intent?.getBooleanExtra(EXTRA_IS_MANUAL, false) ?: false

        if (taskId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground immediately
        val initialNotification = buildNotification("Initializing scheduled file move...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)

        serviceScope.launch {
            try {
                processTask(taskId, isManual)
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                ServiceCompat.stopForeground(this@FileMoverService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processTask(taskId: Long, isManual: Boolean) {
        val dao = MovahDatabase.getDatabase(applicationContext).moveDao()
        val task = dao.getTaskById(taskId) ?: return

        updateNotification("Moving files: ${task.name}")

        val result = FileMoverEngine.executeMove(task.sourcePath, task.targetPath)

        // Write log
        val log = MoveLog(
            taskId = task.id,
            taskName = task.name,
            timestamp = System.currentTimeMillis(),
            status = result.status,
            itemsMoved = result.itemsMoved,
            details = result.details
        )
        dao.insertLog(log)
        dao.trimOldLogs(150)

        // Update task metadata
        dao.updateTask(
            task.copy(
                lastRunTimestamp = System.currentTimeMillis(),
                lastRunStatus = result.status,
                lastRunCount = result.itemsMoved
            )
        )

        // Reschedule next daily execution if not a manual run (or re-arm next daily run regardless)
        if (task.isEnabled) {
            AlarmScheduler.scheduleTask(applicationContext, task)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, MovahApp.CHANNEL_ID)
            .setContentTitle("Movah File Transfer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }
}
