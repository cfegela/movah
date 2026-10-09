package com.cfeg.movah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MoveTask::class, MoveLog::class], version = 1, exportSchema = false)
abstract class MovahDatabase : RoomDatabase() {

    abstract fun moveDao(): MoveDao

    companion object {
        @Volatile
        private var INSTANCE: MovahDatabase? = null

        fun getDatabase(context: Context): MovahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovahDatabase::class.java,
                    "movah_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
