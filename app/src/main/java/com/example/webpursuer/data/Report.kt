package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val customPrompt: String,
    val scheduleHour: Int, // 0-23
    val scheduleMinute: Int = 0, // Default 0
    val monitorIds: String, // Comma separated IDs: "1,2,5" or empty for all
    val enabled: Boolean = true,
    val lastRunTime: Long = 0L
)
