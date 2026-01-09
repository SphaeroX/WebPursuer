package com.murmli.webpursuer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "search_logs",
        foreignKeys =
                [
                        ForeignKey(
                                entity = Search::class,
                                parentColumns = ["id"],
                                childColumns = ["searchId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [androidx.room.Index(value = ["searchId"])]
)
data class SearchLog(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val searchId: Int,
        val timestamp: Long,
        val resultText: String,
        val aiConditionMet: Boolean?
)
