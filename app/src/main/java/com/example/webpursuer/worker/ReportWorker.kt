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
import java.util.Calendar

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
            val reportId = inputData.getInt("report_id", -1)
            if (reportId == -1) {
                // Fallback for legacy global worker if still active, or error
                return Result.failure()
            }

            val report = database.reportDao().getById(reportId)
            if (report == null || !report.enabled) {
                return Result.success()
            }
            
            // Check Schedule Logic
            if (report.scheduleType == "SPECIFIC_TIME") {
                val calendar = Calendar.getInstance()
                // Calendar.SUNDAY = 1, MONDAY = 2 ... SATURDAY = 7
                // Our bitmask: 0=Mon, 1=Tue... 6=Sun? 
                // Let's assume Standard:
                // Bit 0 (1): Monday
                // Bit 1 (2): Tuesday
                // ...
                // Bit 6 (64): Sunday
                
                // Map Calendar day to our bit index (0-6)
                // Calendar.MONDAY (2) -> 0
                // Calendar.TUESDAY (3) -> 1
                // ...
                // Calendar.SUNDAY (1) -> 6
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val bitIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                
                val mask = 1 shl bitIndex
                if ((report.scheduleDays and mask) == 0) {
                   // Not scheduled for today
                   return Result.success()
                }
            }

            val lastTime = report.lastRunTime
            
            // Default to 24 hours ago if never run
            val since = if (lastTime == 0L) {
                System.currentTimeMillis() - 24 * 60 * 60 * 1000
            } else {
                lastTime
            }

            val logs = checkLogDao.getChangedLogsSince(since)
            
            // Filter by selected monitors
            val selectedMonitorIdsStr = report.monitorIds
            val filteredLogs = if (selectedMonitorIdsStr.isEmpty()) {
                logs // If empty, assume all
            } else {
                val ids = selectedMonitorIdsStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                logs.filter { ids.contains(it.monitorId) }
            }

            if (filteredLogs.isEmpty()) {
                // Update time even if empty to avoid backlog
                database.reportDao().update(report.copy(lastRunTime = System.currentTimeMillis()))
                return Result.success()
            }

            // Build Prompt
            val sb = StringBuilder()
            // Add custom instructions if present
            if (report.customPrompt.isNotBlank()) {
                sb.append("Instruction: ${report.customPrompt}\n\n")
            } else {
                sb.append("Please summarize these changes for me.\n\n")
            }

            sb.append("Here is the list of changes detected since ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(since))}:\n\n")
            
            val grouped = filteredLogs.groupBy { it.monitorId }
            
            for ((monitorId, montiorLogs) in grouped) {
                val monitor = monitorDao.getById(monitorId)
                if (monitor != null) {
                    sb.append("Website: ${monitor.name} (${monitor.url})\n")
                    for (log in montiorLogs) {
                        sb.append("- At ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))}: ${log.message}\n")
                        if (!log.content.isNullOrBlank()) {
                            val contentPreview = log.content.take(200).replace("\n", " ")
                            sb.append("  Content snippet: $contentPreview...\n")
                        }
                    }
                    sb.append("\n")
                }
            }

            val generatedReportContent = openRouterService.generateReport(sb.toString())
            
            // Save to database
            val generatedReport = com.example.webpursuer.data.GeneratedReport(
                reportId = report.id,
                timestamp = System.currentTimeMillis(),
                content = generatedReportContent,
                summary = generatedReportContent.take(100) + "..."
            )
            database.generatedReportDao().insert(generatedReport)

            sendReportNotification(report.name)
            
            database.reportDao().update(report.copy(lastRunTime = System.currentTimeMillis()))
            
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
    
    private fun sendReportNotification(title: String) {
        val intent = android.content.Intent(applicationContext, com.example.webpursuer.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(applicationContext, "web_monitor_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Report Available: $title")
            .setContentText("Tap to view the latest report.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(androidx.core.app.NotificationManagerCompat.from(applicationContext)) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(System.currentTimeMillis().toInt(), builder.build()) // Unique ID per notification
            }
        }
    }
}
