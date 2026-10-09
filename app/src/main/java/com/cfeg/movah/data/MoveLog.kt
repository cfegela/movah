package com.cfeg.movah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "move_logs")
data class MoveLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val taskName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "PARTIAL", "FAILED"
    val itemsMoved: Int,
    val details: String = ""
)
