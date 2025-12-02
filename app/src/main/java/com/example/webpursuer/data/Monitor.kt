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
    val enabled: Boolean = true
)
