package com.example.webpursuer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.webpursuer.data.CheckLogDao
import com.example.webpursuer.data.MonitorDao
import com.example.webpursuer.data.SettingsRepository
import com.example.webpursuer.network.OpenRouterService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = com.example.webpursuer.data.AppDatabase.getDatabase(context)
    private val checkLogDao = database.checkLogDao()
    private val monitorDao = database.monitorDao()
    private val settingsRepository = com.example.webpursuer.data.SettingsRepository(context)
    private val openRouterService = com.example.webpursuer.network.OpenRouterService(settingsRepository)

    override suspend fun doWork(): Result {
        try {
            val enabled = settingsRepository.reportEnabled.first()
            if (!enabled) {
                return Result.success()
            }

            val lastTime = settingsRepository.lastReportTime.first()
            
            // Default to 24 hours ago if never run
            val since = if (lastTime == 0L) {
                System.currentTimeMillis() - 24 * 60 * 60 * 1000
            } else {
                lastTime
            }

            val logs = checkLogDao.getChangedLogsSince(since)
            
            // Filter by selected monitors
            val selectedMonitors = settingsRepository.reportMonitorSelection.first()
            val filteredLogs = if (selectedMonitors.isEmpty()) {
                logs // If none selected, assume all (or maybe user wants none? Let's assume all for now as "Standard")
            } else {
                logs.filter { selectedMonitors.contains(it.monitorId.toString()) }
            }

            if (filteredLogs.isEmpty()) {
                // No changes, maybe just update time? Or do nothing.
                // Let's update time so we don't report "no changes" for 100 days at once later.
                settingsRepository.saveLastReportTime(System.currentTimeMillis())
                return Result.success()
            }

            // Build Prompt
            val sb = StringBuilder()
            sb.append("Here is the list of changes detected since ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(since))}:\n\n")
            
            // We need monitor names. Group by monitor ID
            val grouped = filteredLogs.groupBy { it.monitorId }
            
            for ((monitorId, montiorLogs) in grouped) {
                // monitorId is key, which is int
                val monitor = monitorDao.getById(monitorId)
                if (monitor != null) {
                    sb.append("Website: ${monitor.name} (${monitor.url})\n")
                    for (log in montiorLogs) {
                        sb.append("- At ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))}: ${log.message}\n")
                        if (!log.content.isNullOrBlank()) {
                            // Maybe truncate content if too long?
                            val contentPreview = log.content.take(200).replace("\n", " ")
                            sb.append("  Content snippet: $contentPreview...\n")
                        }
                    }
                    sb.append("\n")
                }
            }

            sb.append("\nPlease summarize these changes for me.")

            val report = openRouterService.generateReport(sb.toString())

            sendReportNotification(report)
            
            settingsRepository.saveLastReportTime(System.currentTimeMillis())
            
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
    
    private fun sendReportNotification(report: String) {
        val intent = android.content.Intent(applicationContext, com.example.webpursuer.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(applicationContext, "web_monitor_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily WebPursuer Report")
            .setContentText(report.take(100) + "...") // Preview
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(report))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(androidx.core.app.NotificationManagerCompat.from(applicationContext)) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(9999, builder.build()) // Fixed ID for report
            }
        }
    }
}
