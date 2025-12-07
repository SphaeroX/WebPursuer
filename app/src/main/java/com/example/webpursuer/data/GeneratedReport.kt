package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "generated_reports",
    foreignKeys = [
        ForeignKey(
            entity = Report::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GeneratedReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reportId: Int,
    val timestamp: Long,
    val content: String,
    val summary: String
)
