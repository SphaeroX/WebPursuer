package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val customPrompt: String,
    val scheduleHour: Int, // 0-23 (Start time for interval or specific time)
    val scheduleMinute: Int = 0, // Default 0
    val scheduleType: String = "SPECIFIC_TIME", // "SPECIFIC_TIME" or "INTERVAL"
    val scheduleDays: Int = 127, // Bitmask for Mon-Sun (Default all days: 1111111)
    val intervalHours: Int = 24, // For INTERVAL type
    val monitorIds: String, // Comma separated IDs
    val enabled: Boolean = true,
    val lastRunTime: Long = 0L
)
