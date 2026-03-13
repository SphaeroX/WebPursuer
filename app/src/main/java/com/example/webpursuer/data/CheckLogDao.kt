package com.murmli.webpursuer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {
        @Query("SELECT * FROM check_logs WHERE monitorId = :monitorId ORDER BY timestamp DESC")
        fun getLogsForMonitor(monitorId: Int): Flow<List<CheckLog>>

        @Query(
                "SELECT * FROM check_logs WHERE monitorId = :monitorId AND result != 'UNCHANGED' ORDER BY timestamp DESC"
        )
        fun getLogsForMonitorFiltered(monitorId: Int): Flow<List<CheckLog>>

        @Query("SELECT * FROM check_logs WHERE timestamp > :since AND result = 'CHANGED'")
        suspend fun getChangedLogsSince(since: Long): List<CheckLog>

        @Query("SELECT * FROM check_logs WHERE id = :id") suspend fun getById(id: Int): CheckLog?

        @Query(
                "SELECT * FROM check_logs WHERE monitorId = :monitorId AND timestamp < :timestamp ORDER BY timestamp DESC LIMIT 1"
        )
        suspend fun getPreviousLog(monitorId: Int, timestamp: Long): CheckLog?

        @Query(
                "SELECT * FROM check_logs WHERE monitorId = :monitorId AND result = 'CHANGED' ORDER BY timestamp DESC LIMIT 1"
        )
        suspend fun getLastChangedLog(monitorId: Int): CheckLog?

        @Insert suspend fun insert(log: CheckLog): Long

        @Query("DELETE FROM check_logs WHERE monitorId = :monitorId")
        suspend fun deleteLogsForMonitor(monitorId: Int)

        @Query("SELECT * FROM check_logs WHERE result = 'CHANGED' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
        suspend fun getRecentChangesPaged(limit: Int, offset: Int): List<CheckLog>

        @Query("SELECT * FROM check_logs WHERE result = 'CHANGED' ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
        suspend fun getRecentChangesPagedAsc(limit: Int, offset: Int): List<CheckLog>

        @Query("SELECT COUNT(*) FROM check_logs WHERE result = 'CHANGED'")
        suspend fun getTotalChangedCount(): Int

        @Query("""
            SELECT cl.* FROM check_logs cl
            JOIN monitors m ON cl.monitorId = m.id
            WHERE cl.result = 'CHANGED'
            AND (:onlyOverThreshold = 0 OR (
                (m.thresholdType = 'PERCENTAGE' AND cl.changePercentage >= m.thresholdValue) OR
                (m.thresholdType = 'CHARACTER_COUNT' AND cl.changePercentage >= 0) -- Character count is harder without the actual diff, but we can't do much here without schema change
            ))
            AND (:minChange IS NULL OR cl.changePercentage >= :minChange)
            AND (:startDate IS NULL OR cl.timestamp >= :startDate)
            AND (:endDate IS NULL OR cl.timestamp <= :endDate)
            ORDER BY 
                CASE WHEN :sortOrder = 'ASC' THEN cl.timestamp END ASC,
                CASE WHEN :sortOrder = 'DESC' THEN cl.timestamp END DESC
            LIMIT :limit OFFSET :offset
        """)
        suspend fun getRecentChangesFiltered(
            limit: Int, 
            offset: Int, 
            sortOrder: String,
            onlyOverThreshold: Int, // 1 for true, 0 for false
            minChange: Double?,
            startDate: Long?,
            endDate: Long?
        ): List<CheckLog>

        @Query("""
            SELECT COUNT(cl.id) FROM check_logs cl
            JOIN monitors m ON cl.monitorId = m.id
            WHERE cl.result = 'CHANGED'
            AND (:onlyOverThreshold = 0 OR (
                (m.thresholdType = 'PERCENTAGE' AND cl.changePercentage >= m.thresholdValue) OR
                (m.thresholdType = 'CHARACTER_COUNT' AND cl.changePercentage >= 0)
            ))
            AND (:minChange IS NULL OR cl.changePercentage >= :minChange)
            AND (:startDate IS NULL OR cl.timestamp >= :startDate)
            AND (:endDate IS NULL OR cl.timestamp <= :endDate)
        """)
        suspend fun getTotalChangedCountFiltered(
            onlyOverThreshold: Int,
            minChange: Double?,
            startDate: Long?,
            endDate: Long?
        ): Int
}
