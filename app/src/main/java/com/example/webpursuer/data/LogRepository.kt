package com.example.webpursuer.data

import kotlinx.coroutines.flow.Flow

class LogRepository(private val appLogDao: AppLogDao) {
    val allLogs: Flow<List<AppLog>> = appLogDao.getAllLogs()
    
    fun getErrorLogs(): Flow<List<AppLog>> = appLogDao.getErrorLogs()

    suspend fun log(type: String, message: String, isError: Boolean = false, details: String? = null) {
        val log = AppLog(
            timestamp = System.currentTimeMillis(),
            type = type,
            message = message,
            isError = isError,
            details = details
        )
        appLogDao.insert(log)
    }

    suspend fun logInfo(type: String, message: String) {
        log(type, message, isError = false)
    }

    suspend fun logError(type: String, message: String, details: String? = null) {
        log(type, message, isError = true, details = details)
    }

    suspend fun clearLogs() {
        appLogDao.deleteAll()
    }
}
