package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_logs",
    foreignKeys = [
        ForeignKey(
            entity = Monitor::class,
            parentColumns = ["id"],
            childColumns = ["monitorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CheckLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monitorId: Int,
    val timestamp: Long,
    val result: String, // "SUCCESS", "FAILURE", "CHANGED", "UNCHANGED"
    val message: String,
    val content: String? = null
)
