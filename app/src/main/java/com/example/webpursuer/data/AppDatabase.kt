package com.example.webpursuer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [Monitor::class, CheckLog::class, Interaction::class, Report::class, GeneratedReport::class, AppLog::class], version = 8, exportSchema = false)
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
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {
                            db.execSQL(
                                "INSERT INTO monitors (url, name, selector, checkIntervalMinutes, lastCheckTime, enabled, llmEnabled, notificationsEnabled, scheduleType) VALUES " +
                                        "('https://www.unixtimestamp.com/', 'Unix Timestamp', '#main-segment > div.box > div > div.ui.two.column.grid > div > div:nth-child(2) > div > div.value.epoch', 15, 0, 1, 0, 1, 'INTERVAL')"
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
