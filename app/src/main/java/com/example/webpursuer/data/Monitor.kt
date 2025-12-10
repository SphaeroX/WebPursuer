package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitors")
data class Monitor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val name: String,
    val selector: String, // CSS Selector or XPath
    val checkIntervalMinutes: Long = 15,
    val lastCheckTime: Long = 0,
    val lastContentHash: String? = null,
    val enabled: Boolean = true,
    val llmPrompt: String? = null,
    val llmEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val scheduleType: String = "INTERVAL", // INTERVAL or DAILY
    val checkTime: String? = null, // HH:mm for DAILY
    @androidx.room.ColumnInfo(defaultValue = "0") val useAiInterpreter: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "'Summarize the key information.'") val aiInterpreterInstruction: String = "Summarize the key information."
)
