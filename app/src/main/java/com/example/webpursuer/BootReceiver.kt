package com.murmli.webpursuer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "System boot completed - restoring all workers")
            
            val appScope = CoroutineScope(Dispatchers.Default)
            appScope.launch {
                try {
                    restoreWebCheckWorker(context)
                    restoreSearchWorkers(context)
                    restoreReportWorkers(context)
                    Log.d("BootReceiver", "All workers restored after boot")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring workers: ${e.message}", e)
                }
            }
        }
    }
    
    private fun restoreWebCheckWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WebCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "WebCheckWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        Log.d("BootReceiver", "WebCheckWorker restored")
    }
    
    private suspend fun restoreSearchWorkers(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val searchDao = database.searchDao()
        
        val searches = searchDao.getAllEnabledSync()
        
        for (search in searches) {
            if (!search.enabled) continue
            
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
                PeriodicWorkRequestBuilder<SearchWorker>(interval, TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            Log.d("BootReceiver", "SearchWorker restored for search ${search.id}")
        }
    }
    
    private suspend fun restoreReportWorkers(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val reportDao = database.reportDao()
        
        val reports = reportDao.getAllEnabledSync()
        
        for (report in reports) {
            if (!report.enabled) continue
            
            val workName = "ReportWorker_${report.id}"
            
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
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    workName,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            Log.d("BootReceiver", "ReportWorker restored for report ${report.id}")
        }
    }
}
