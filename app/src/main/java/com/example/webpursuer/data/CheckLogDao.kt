package com.example.webpursuer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {
    @Query("SELECT * FROM check_logs WHERE monitorId = :monitorId ORDER BY timestamp DESC")
    fun getLogsForMonitor(monitorId: Int): Flow<List<CheckLog>>

    @Query("SELECT * FROM check_logs WHERE timestamp > :since AND result = 'CHANGED'")
    suspend fun getChangedLogsSince(since: Long): List<CheckLog>

    @Query("SELECT * FROM check_logs WHERE id = :id")
    suspend fun getById(id: Int): CheckLog?

    @Query("SELECT * FROM check_logs WHERE monitorId = :monitorId AND timestamp < :timestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun getPreviousLog(monitorId: Int, timestamp: Long): CheckLog?

    @Insert
    suspend fun insert(log: CheckLog): Long
    
    @Query("DELETE FROM check_logs WHERE monitorId = :monitorId")
    suspend fun deleteLogsForMonitor(monitorId: Int)
}
