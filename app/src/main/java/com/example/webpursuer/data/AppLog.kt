package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // MONITOR, LLM, REPORT, ERROR, SYSTEM
    val message: String,
    val isError: Boolean = false,
    val details: String? = null
)
