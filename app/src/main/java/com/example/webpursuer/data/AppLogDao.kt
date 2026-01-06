package com.murmli.webpursuer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC") fun getAllLogs(): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE isError = 1 ORDER BY timestamp DESC")
    fun getErrorLogs(): Flow<List<AppLog>>

    @Insert suspend fun insert(log: AppLog)

    @Query("DELETE FROM app_logs") suspend fun deleteAll()

    @Query("DELETE FROM app_logs WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
