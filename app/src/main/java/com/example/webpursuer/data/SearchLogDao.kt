package com.murmli.webpursuer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchLogDao {
    @Query("SELECT * FROM search_logs WHERE searchId = :searchId ORDER BY timestamp DESC")
    fun getLogsForSearch(searchId: Int): Flow<List<SearchLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLog(log: SearchLog)

    @Delete suspend fun deleteLog(log: SearchLog)

    @Query("DELETE FROM search_logs WHERE searchId = :searchId")
    suspend fun deleteLogsBySearchId(searchId: Int)
}
