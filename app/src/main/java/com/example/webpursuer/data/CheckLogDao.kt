package com.example.webpursuer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {
    @Query("SELECT * FROM check_logs WHERE monitorId = :monitorId ORDER BY timestamp DESC")
    fun getLogsForMonitor(monitorId: Int): Flow<List<CheckLog>>

    @Insert
    suspend fun insert(log: CheckLog)
    
    @Query("DELETE FROM check_logs WHERE monitorId = :monitorId")
    suspend fun deleteLogsForMonitor(monitorId: Int)
}
