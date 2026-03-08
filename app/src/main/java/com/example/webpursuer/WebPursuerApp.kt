package com.murmli.webpursuer

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.murmli.webpursuer.data.AppDatabase
import com.murmli.webpursuer.worker.WebCheckWorker
import com.murmli.webpursuer.worker.SearchWorker
import com.murmli.webpursuer.worker.ReportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder

class WebPursuerApp : Application() {

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        restoreAllWorkers()
    }
    
    private fun restoreAllWorkers() {
        appScope.launch {
            try {
                restoreWebCheckWorkers()
                restoreSearchWorkers()
                restoreReportWorkers()
                Log.d("WebPursuerApp", "All workers restored successfully")
            } catch (e: Exception) {
                Log.e("WebPursuerApp", "Error restoring workers: ${e.message}", e)
            }
        }
    }
    
    private suspend fun restoreWebCheckWorkers() {
        val database = AppDatabase.getDatabase(this)
        val monitors = database.monitorDao().getAllSync()
        
        // Cancel old legacy global worker if it exists
        WorkManager.getInstance(this).cancelUniqueWork("WebCheckWork")

        for (monitor in monitors) {
            if (monitor.enabled) {
                WebCheckWorker.scheduleMonitor(this, monitor)
            } else {
                WebCheckWorker.cancelMonitor(this, monitor.id)
            }
        }
        Log.d("WebPursuerApp", "Individual WebCheckWorkers restored")
    }
    
    private suspend fun restoreSearchWorkers() {
        val database = AppDatabase.getDatabase(this)
        val searchDao = database.searchDao()
        
        val searches = searchDao.getAllEnabledSync()
        
        for (search in searches) {
            if (!search.enabled) {
                WorkManager.getInstance(this).cancelUniqueWork("search_${search.id}")
                continue
            }
            
            val inputData = androidx.work.workDataOf("searchId" to search.id)
            val uniqueWorkName = "search_${search.id}"
            
            val workRequest = if (search.scheduleType == "SPECIFIC_TIME") {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, search.scheduleHour)
                    set(Calendar.MINUTE, search.scheduleMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                val initialDelay = target.timeInMillis - now.timeInMillis
                
                PeriodicWorkRequestBuilder<SearchWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            } else {
                val interval = search.intervalMinutes.coerceAtLeast(15)
                PeriodicWorkRequestBuilder<SearchWorker>(interval.toLong(), TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }
            
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            Log.d("WebPursuerApp", "SearchWorker restored for search ${search.id}")
        }
    }
    
    private suspend fun restoreReportWorkers() {
        val database = AppDatabase.getDatabase(this)
        val reportDao = database.reportDao()
        
        val reports = reportDao.getAllEnabledSync()
        
        for (report in reports) {
            val workName = "ReportWorker_${report.id}"
            if (!report.enabled) {
                WorkManager.getInstance(this).cancelUniqueWork(workName)
                continue
            }
            
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, report.scheduleHour)
                set(Calendar.MINUTE, report.scheduleMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (target.before(now)) {
                if (report.scheduleType == "INTERVAL") {
                    val intervalMillis: Long = report.intervalHours.toLong() * 60L * 60L * 1000L
                    while (target.before(now)) {
                        val newTime: Long = target.timeInMillis + intervalMillis
                        target.timeInMillis = newTime
                    }
                } else {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            val initialDelay = target.timeInMillis - now.timeInMillis
            
            val workRequest = if (report.scheduleType == "INTERVAL") {
                PeriodicWorkRequestBuilder<ReportWorker>(
                    report.intervalHours.toLong(),
                    TimeUnit.HOURS
                )
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(androidx.work.workDataOf("report_id" to report.id))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            } else {
                PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(androidx.work.workDataOf("report_id" to report.id))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }
            
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    workName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            Log.d("WebPursuerApp", "ReportWorker restored for report ${report.id}")
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Web Monitor Updates"
            val descriptionText = "Notifications for web page changes"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                    android.app.NotificationChannel("web_monitor_channel", name, importance).apply {
                        description = descriptionText
                    }
            val notificationManager: android.app.NotificationManager =
                    getSystemService(android.content.Context.NOTIFICATION_SERVICE) as
                            android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
