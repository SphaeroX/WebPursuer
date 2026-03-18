package com.murmli.webpursuer.data

import kotlinx.coroutines.flow.Flow

class LogRepository(private val appLogDao: AppLogDao) {
    val allLogs: Flow<List<AppLog>> = appLogDao.getAllLogs()

    fun getErrorLogs(): Flow<List<AppLog>> = appLogDao.getErrorLogs()

    suspend fun log(
            type: String,
            message: String,
            isError: Boolean = false,
            details: String? = null,
            monitorId: Int? = null
    ) {
        val log =
                AppLog(
                        timestamp = System.currentTimeMillis(),
                        type = type,
                        message = message,
                        isError = isError,
                        details = details,
                        monitorId = monitorId
                )
        appLogDao.insert(log)
    }

    suspend fun logInfo(type: String, message: String, monitorId: Int? = null) {
        log(type, message, isError = false, monitorId = monitorId)
    }

    suspend fun logError(type: String, message: String, details: String? = null, monitorId: Int? = null) {
        log(type, message, isError = true, details = details, monitorId = monitorId)
    }

    suspend fun clearLogs() {
        appLogDao.deleteAll()
    }
}
