package com.example.webpursuer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

import androidx.room.AutoMigration

@Database(
    entities = [Monitor::class, CheckLog::class, Interaction::class, Report::class, GeneratedReport::class, AppLog::class],
    version = 9,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 8, to = 9)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitorDao(): MonitorDao
    abstract fun checkLogDao(): CheckLogDao
    abstract fun interactionDao(): InteractionDao
    abstract fun reportDao(): ReportDao
    abstract fun generatedReportDao(): GeneratedReportDao
    abstract fun appLogDao(): AppLogDao

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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
