package com.cfeg.movah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MoveTask::class, MoveLog::class], version = 2, exportSchema = false)
abstract class MovahDatabase : RoomDatabase() {

    abstract fun moveDao(): MoveDao

    companion object {
        @Volatile
        private var INSTANCE: MovahDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE move_tasks ADD COLUMN minAgeDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): MovahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovahDatabase::class.java,
                    "movah_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
