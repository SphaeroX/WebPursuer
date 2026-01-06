package com.murmli.webpursuer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedReportDao {
    @Query("SELECT * FROM generated_reports WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getAllForReport(reportId: Int): Flow<List<GeneratedReport>>

    @Query("SELECT * FROM generated_reports WHERE id = :id")
    suspend fun getById(id: Int): GeneratedReport?

    @Insert suspend fun insert(generatedReport: GeneratedReport): Long

    @Delete suspend fun delete(generatedReport: GeneratedReport)
}
