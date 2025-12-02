package com.example.webpursuer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions WHERE monitorId = :monitorId ORDER BY orderIndex ASC")
    suspend fun getInteractionsForMonitor(monitorId: Int): List<Interaction>

    @Insert
    suspend fun insertAll(interactions: List<Interaction>)
    
    @Query("DELETE FROM interactions WHERE monitorId = :monitorId")
    suspend fun deleteInteractionsForMonitor(monitorId: Int)
}
