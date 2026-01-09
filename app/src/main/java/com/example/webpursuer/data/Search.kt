package com.murmli.webpursuer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "searches")
data class Search(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val title: String,
        val prompt: String,
        val scheduleType: String = "INTERVAL", // INTERVAL, SPECIFIC_TIME
        val intervalMinutes: Long = 60,
        val enabled: Boolean = true,
        val lastRunTime: Long = 0,
        val notificationEnabled: Boolean = true,
        val aiConditionEnabled: Boolean = false,
        val aiConditionPrompt: String? = null,
        val scheduleHour: Int = 0,
        val scheduleMinute: Int = 0,
        val scheduleDays: Int = 127 // Bitmask for Mon-Sun
)
