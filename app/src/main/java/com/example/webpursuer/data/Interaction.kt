package com.example.webpursuer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactions",
    foreignKeys = [
        ForeignKey(
            entity = Monitor::class,
            parentColumns = ["id"],
            childColumns = ["monitorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monitorId: Int,
    val type: String, // "click", "input", "wait"
    val selector: String,
    val value: String? = null,
    val orderIndex: Int
)
