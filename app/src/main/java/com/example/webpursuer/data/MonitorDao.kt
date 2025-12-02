package com.example.webpursuer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorDao {
    @Query("SELECT * FROM monitors")
    fun getAll(): Flow<List<Monitor>>

    @Query("SELECT * FROM monitors")
    suspend fun getAllSync(): List<Monitor>

    @Query("SELECT * FROM monitors WHERE id = :id")
    suspend fun getById(id: Int): Monitor?

    @Insert
    suspend fun insertAndReturnId(monitor: Monitor): Long

    @Insert
    suspend fun insert(monitor: Monitor)

    @Update
    suspend fun update(monitor: Monitor)

    @Delete
    suspend fun delete(monitor: Monitor)
}
