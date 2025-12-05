package com.example.webpursuer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Monitor::class, CheckLog::class, Interaction::class, Report::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitorDao(): MonitorDao
    abstract fun checkLogDao(): CheckLogDao
    abstract fun interactionDao(): InteractionDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webpursuer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
