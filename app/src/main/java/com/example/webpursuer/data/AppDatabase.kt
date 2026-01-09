package com.murmli.webpursuer.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
        entities =
                [
                        Monitor::class,
                        CheckLog::class,
                        Interaction::class,
                        Report::class,
                        GeneratedReport::class,
                        AppLog::class],
        version = 12,
        exportSchema = true,
        autoMigrations =
                [
                        AutoMigration(from = 8, to = 9),
                        AutoMigration(from = 9, to = 10),
                        AutoMigration(from = 10, to = 11),
                        AutoMigration(from = 11, to = 12)]
)
abstract class AppDatabase : RoomDatabase() {
        abstract fun monitorDao(): MonitorDao
        abstract fun checkLogDao(): CheckLogDao
        abstract fun interactionDao(): InteractionDao
        abstract fun reportDao(): ReportDao
        abstract fun generatedReportDao(): GeneratedReportDao
        abstract fun appLogDao(): AppLogDao

        companion object {
                @Volatile private var INSTANCE: AppDatabase? = null

                fun getDatabase(context: Context): AppDatabase {
                        return INSTANCE
                                ?: synchronized(this) {
                                        val instance =
                                                Room.databaseBuilder(
                                                                context.applicationContext,
                                                                AppDatabase::class.java,
                                                                "webpursuer_database"
                                                        )
                                                        .build()
                                        INSTANCE = instance
                                        instance
                                }
                }

                fun closeAndReset() {
                        try {
                                if (INSTANCE?.isOpen == true) {
                                        INSTANCE?.close()
                                }
                        } catch (e: Exception) {
                                e.printStackTrace()
                        }
                        INSTANCE = null
                }
        }
}
