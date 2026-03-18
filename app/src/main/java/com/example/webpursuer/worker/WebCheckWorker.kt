package com.murmli.webpursuer.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.murmli.webpursuer.data.AppDatabase
import com.murmli.webpursuer.data.Monitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WebCheckWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val monitorDao = database.monitorDao()
    private val checkLogDao = database.checkLogDao()
    private val interactionDao = database.interactionDao()
    private val settingsRepository = com.murmli.webpursuer.data.SettingsRepository(context)
    private val logRepository = com.murmli.webpursuer.data.LogRepository(database.appLogDao())
    private val openRouterService =
            com.murmli.webpursuer.network.OpenRouterService(settingsRepository, logRepository)
    private val webChecker =
            WebChecker(
                    context.applicationContext,
                    monitorDao,
                    checkLogDao,
                    interactionDao,
                    openRouterService,
                    settingsRepository,
                    logRepository
            )

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                val monitorId = inputData.getInt("monitorId", -1)
                
                if (!com.murmli.webpursuer.util.NetworkUtils.isNetworkAvailable(applicationContext)
                ) {
                    logRepository.logInfo("SYSTEM", "Skipping monitor check: No network available")
                    return@withContext Result.retry()
                }

                try {
                    val monitor = if (monitorId != -1) monitorDao.getById(monitorId) else null
                    val isSpecificTime = monitor?.scheduleType == "SPECIFIC_TIME" || monitor?.scheduleType == "DAILY"

                    // Check for quiet time
                    val isQuietEnabled = settingsRepository.workerQuietEnabled.first()
                    if (isQuietEnabled) {
                        val start = settingsRepository.workerQuietStartHour.first()
                        val end = settingsRepository.workerQuietEndHour.first()
                        if (settingsRepository.isQuietTime(start, end)) {
                            Log.d("WebCheckWorker", "Skipping worker: Quiet time active ($start to $end)")
                            // If it's a specific time monitor, we should retry so it runs after quiet time
                            // instead of skipping the entire day.
                            return@withContext if (isSpecificTime) Result.retry() else Result.success()
                        }
                    }

                    if (monitorId != -1) {
                        if (monitor != null && monitor.enabled) {
                            if (shouldRunNow(monitor)) {
                                logRepository.logInfo(
                                    "MONITOR",
                                    "Checking monitor: ${monitor.name} (${monitor.url})"
                                )
                                try {
                                    webChecker.checkMonitor(monitor, System.currentTimeMillis())
                                } catch (e: NetworkException) {
                                    logRepository.logInfo("MONITOR", "Network error checking ${monitor.name}: ${e.message}. Retrying via WorkManager.")
                                    return@withContext Result.retry()
                                }
                            } else {
                                Log.d("WebCheckWorker", "Skipping monitor ${monitor.name}: Not scheduled for today (Bitmask: ${monitor.scheduleDays})")
                            }
                        } else {
                            Log.d("WebCheckWorker", "Monitor $monitorId not found or disabled")
                        }
                    } else {
                        // Legacy support
                        logRepository.logInfo("SYSTEM", "Running legacy WebCheckWorker (checking all monitors)")
                        val monitors = monitorDao.getAllSync()
                        val now = System.currentTimeMillis()
                        var anyNetworkError = false
                        for (monitorItem in monitors) {
                            if (shouldRunNow(monitorItem)) {
                                try {
                                    webChecker.checkMonitor(monitorItem, now)
                                } catch (e: NetworkException) {
                                    logRepository.logInfo("MONITOR", "Network error for ${monitorItem.name}: ${e.message}")
                                    anyNetworkError = true
                                } catch (e: Exception) {
                                    logRepository.logError("MONITOR", "Error: ${e.message}")
                                }
                            }
                        }
                        if (anyNetworkError) return@withContext Result.retry()
                    }
                    Result.success()
                } catch (e: Exception) {
                    logRepository.logError(
                            "SYSTEM",
                            "WebCheckWorker failed: ${e.message}",
                            e.stackTraceToString()
                    )
                    Result.retry()
                }
            }

    private fun shouldRunNow(monitor: Monitor): Boolean {
        if (monitor.scheduleType != "SPECIFIC_TIME" && monitor.scheduleType != "DAILY") return true
        
        val calendar = Calendar.getInstance()
        // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to our index (Mon=0..Sun=6)
        val dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val isEnabled = (monitor.scheduleDays and (1 shl dayIndex)) != 0
        Log.d("WebCheckWorker", "Checking schedule for ${monitor.name}: DayIndex=$dayIndex, Bitmask=${monitor.scheduleDays}, Enabled=$isEnabled")
        return isEnabled
    }

    companion object {
        fun scheduleMonitor(context: Context, monitor: Monitor) {
            if (!monitor.enabled) {
                cancelMonitor(context, monitor.id)
                return
            }

            val workManager = WorkManager.getInstance(context)
            val uniqueWorkName = "monitor_check_${monitor.id}"
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf("monitorId" to monitor.id)
            val isSpecificTime = monitor.scheduleType == "SPECIFIC_TIME" || monitor.scheduleType == "DAILY"

            val workRequest = if (isSpecificTime) {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, monitor.scheduleHour)
                    set(Calendar.MINUTE, monitor.scheduleMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                val initialDelay = target.timeInMillis - now.timeInMillis
                
                // For specific time, we use a slightly larger backoff to avoid spamming if it hits quiet time
                PeriodicWorkRequestBuilder<WebCheckWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            } else {
                val interval = monitor.checkIntervalMinutes.coerceAtLeast(15)
                PeriodicWorkRequestBuilder<WebCheckWorker>(interval, TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }

            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE, // UPDATE ensures settings change take effect
                workRequest
            )
            Log.d("WebCheckWorker", "Scheduled monitor ${monitor.id} (${monitor.name})")
        }

        fun cancelMonitor(context: Context, monitorId: Int) {
            val workManager = WorkManager.getInstance(context)
            val uniqueWorkName = "monitor_check_$monitorId"
            workManager.cancelUniqueWork(uniqueWorkName)
            Log.d("WebCheckWorker", "Cancelled monitor $monitorId")
        }
    }
}
