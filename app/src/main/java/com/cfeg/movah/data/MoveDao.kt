package com.cfeg.movah.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoveDao {

    @Query("SELECT * FROM move_tasks ORDER BY id DESC")
    fun getAllTasksFlow(): Flow<List<MoveTask>>

    @Query("SELECT * FROM move_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): MoveTask?

    @Query("SELECT * FROM move_tasks WHERE isEnabled = 1")
    suspend fun getEnabledTasks(): List<MoveTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: MoveTask): Long

    @Update
    suspend fun updateTask(task: MoveTask)

    @Delete
    suspend fun deleteTask(task: MoveTask)

    @Query("SELECT * FROM move_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<MoveLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MoveLog): Long

    @Query("DELETE FROM move_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM move_logs WHERE id IN (SELECT id FROM move_logs ORDER BY timestamp DESC LIMIT -1 OFFSET :maxEntries)")
    suspend fun trimOldLogs(maxEntries: Int = 100)
}
