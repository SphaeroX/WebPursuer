package com.example.webpursuer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.CheckLog
import com.example.webpursuer.data.Monitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.security.MessageDigest

class WebCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val monitorDao = database.monitorDao()
    private val checkLogDao = database.checkLogDao()
    private val interactionDao = database.interactionDao()
    private val settingsRepository = com.example.webpursuer.data.SettingsRepository(context)
    private val logRepository = com.example.webpursuer.data.LogRepository(database.appLogDao())
    private val openRouterService = com.example.webpursuer.network.OpenRouterService(settingsRepository, logRepository)
    private val webChecker = WebChecker(context.applicationContext, monitorDao, checkLogDao, interactionDao, openRouterService, settingsRepository, logRepository)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!com.example.webpursuer.util.NetworkUtils.isNetworkAvailable(applicationContext)) {
            logRepository.logInfo("SYSTEM", "Skipping monitor check: No network available")
            return@withContext Result.retry()
        }
        try {
            logRepository.logInfo("SYSTEM", "Starting monitor checks worker")
            val monitors = monitorDao.getAllSync() // We need a sync version or collect the flow
            val now = System.currentTimeMillis()

            for (monitor in monitors) {
                if (shouldCheck(monitor, now)) {
                    logRepository.logInfo("MONITOR", "Checking monitor: ${monitor.name} (${monitor.url})")
                    try {
                        webChecker.checkMonitor(monitor, now)
                        // Success logging is handled inside WebChecker or we can log here if it returns status
                    } catch (e: Exception) {
                        logRepository.logError("MONITOR", "Error checking monitor ${monitor.name}: ${e.message}", e.stackTraceToString())
                    }
                }
            }
            logRepository.logInfo("SYSTEM", "Monitor checks worker finished")
            Result.success()
        } catch (e: Exception) {
            logRepository.logError("SYSTEM", "WebCheckWorker failed: ${e.message}", e.stackTraceToString())
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun shouldCheck(monitor: Monitor, now: Long): Boolean {
        if (!monitor.enabled) return false

        if (monitor.scheduleType == "DAILY" && monitor.checkTime != null) {
            val parts = monitor.checkTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0

                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = now
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                calendar.set(java.util.Calendar.MINUTE, minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)

                val targetTimeToday = calendar.timeInMillis
                
                // If it's already past the target time for today
                if (now >= targetTimeToday) {
                    // Check if we haven't checked since the target time
                    return monitor.lastCheckTime < targetTimeToday
                }
                return false
            }
        }
        
        // Fallback or INTERVAL type
        val intervalMillis = monitor.checkIntervalMinutes * 60 * 1000
        return (now - monitor.lastCheckTime) >= intervalMillis
    }
}
