package com.murmli.webpursuer.data

import kotlinx.coroutines.flow.Flow

class SearchRepository(private val searchDao: SearchDao, private val searchLogDao: SearchLogDao) {
    val allSearches: Flow<List<Search>> = searchDao.getAllSearches()

    fun getLogsForSearch(searchId: Int): Flow<List<SearchLog>> {
        return searchLogDao.getLogsForSearch(searchId)
    }

    suspend fun getSearchById(id: Int): Search? {
        return searchDao.getSearchById(id)
    }

    suspend fun getEnabledSearches(): List<Search> {
        return searchDao.getEnabledSearches()
    }

    suspend fun addSearch(search: Search): Long {
        return searchDao.insertSearch(search)
    }

    suspend fun updateSearch(search: Search) {
        searchDao.updateSearch(search)
    }

    suspend fun deleteSearch(search: Search) {
        // Logs are deleted via CASCADE
        searchDao.deleteSearch(search)
    }

    suspend fun logResult(searchId: Int, result: String, aiConditionMet: Boolean? = null) {
        val log =
                SearchLog(
                        searchId = searchId,
                        timestamp = System.currentTimeMillis(),
                        resultText = result,
                        aiConditionMet = aiConditionMet
                )
        searchLogDao.insertLog(log)
    }

    suspend fun clearLogs(searchId: Int) {
        searchLogDao.deleteLogsBySearchId(searchId)
    }
}
