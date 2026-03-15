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
                    // Check for quiet time
                    val isQuietEnabled = settingsRepository.workerQuietEnabled.kotlinx.coroutines.flow.first()
                    if (isQuietEnabled) {
                        val start = settingsRepository.workerQuietStartHour.kotlinx.coroutines.flow.first()
                        val end = settingsRepository.workerQuietEndHour.kotlinx.coroutines.flow.first()
                        if (settingsRepository.isQuietTime(start, end)) {
                            Log.d("WebCheckWorker", "Skipping worker: Quiet time active ($start to $end)")
                            return@withContext Result.success() // Or retry? Success is safer to avoid looping
                        }
                    }

                    if (monitorId != -1) {
                        val monitor = monitorDao.getById(monitorId)
                        if (monitor != null && monitor.enabled) {
                            if (shouldRunNow(monitor)) {
                                logRepository.logInfo(
                                    "MONITOR",
                                    "Checking monitor: ${monitor.name} (${monitor.url})"
                                )
                                webChecker.checkMonitor(monitor, System.currentTimeMillis())
                            } else {
                                Log.d("WebCheckWorker", "Skipping monitor ${monitor.name}: Not scheduled for now")
                            }
                        } else {
                            Log.d("WebCheckWorker", "Monitor $monitorId not found or disabled")
                        }
                    } else {
                        // Legacy support: check all (but this is what we want to avoid)
                        logRepository.logInfo("SYSTEM", "Running legacy WebCheckWorker (checking all monitors)")
                        val monitors = monitorDao.getAllSync()
                        val now = System.currentTimeMillis()
                        for (monitor in monitors) {
                            if (shouldRunNow(monitor)) {
                                try {
                                    webChecker.checkMonitor(monitor, now)
                                } catch (e: Exception) {
                                    logRepository.logError("MONITOR", "Error: ${e.message}")
                                }
                            }
                        }
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
        if (monitor.scheduleType != "SPECIFIC_TIME") return true
        
        val calendar = Calendar.getInstance()
        val dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return (monitor.scheduleDays and (1 shl dayIndex)) != 0
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

            val workRequest = if (monitor.scheduleType == "SPECIFIC_TIME") {
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
                
                PeriodicWorkRequestBuilder<WebCheckWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
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
