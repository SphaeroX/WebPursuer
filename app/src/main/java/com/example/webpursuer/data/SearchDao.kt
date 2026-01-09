package com.murmli.webpursuer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query("SELECT * FROM searches ORDER BY id DESC") fun getAllSearches(): Flow<List<Search>>

    @Query("SELECT * FROM searches WHERE id = :id") suspend fun getSearchById(id: Int): Search?

    @Query("SELECT * FROM searches WHERE enabled = 1")
    suspend fun getEnabledSearches(): List<Search>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSearch(search: Search): Long

    @Update suspend fun updateSearch(search: Search)

    @Delete suspend fun deleteSearch(search: Search)
}
