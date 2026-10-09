package com.cfeg.movah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "move_tasks")
data class MoveTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sourcePath: String,
    val targetPath: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long? = null,
    val lastRunStatus: String? = null,
    val lastRunCount: Int? = null
)
