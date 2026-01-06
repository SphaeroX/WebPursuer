package com.murmli.webpursuer.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.murmli.webpursuer.worker.ReportWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

class ReportRepository(private val context: Context, private val reportDao: ReportDao) {

    val allReports: Flow<List<Report>> = reportDao.getAll()

    suspend fun getReport(id: Int): Report? {
        return reportDao.getById(id)
    }

    suspend fun insertReport(report: Report) {
        val id = reportDao.insert(report)
        // Schedule worker for the new report (using the generated ID)
        val insertedReport = report.copy(id = id.toInt())
        scheduleReportWorker(insertedReport)
    }

    suspend fun updateReport(report: Report) {
        reportDao.update(report)
        scheduleReportWorker(report)
    }

    suspend fun deleteReport(report: Report) {
        reportDao.delete(report)
        cancelReportWorker(report)
    }

    private fun scheduleReportWorker(report: Report) {
        val workManager = WorkManager.getInstance(context)
        val workName = "ReportWorker_${report.id}"

        if (!report.enabled) {
            workManager.cancelUniqueWork(workName)
            return
        }

        // Calculate initial delay
        val now = Calendar.getInstance()
        val target =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, report.scheduleHour)
                    set(Calendar.MINUTE, report.scheduleMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

        if (target.before(now)) {
            // For Interval: If start time passed, we might want to start "next interval slot" or
            // just now + interval?
            // "Specific Time" logic: Next day.
            // "Interval" logic: If user says start at 8:00 and run every 3h, and it's 10:00.
            // 8:00 passed. Next run should be 11:00.
            if (report.scheduleType == "INTERVAL") {
                val intervalMillis = report.intervalHours * 60 * 60 * 1000L
                while (target.before(now)) {
                    target.timeInMillis += intervalMillis
                }
            } else {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest =
                if (report.scheduleType == "INTERVAL") {
                    PeriodicWorkRequestBuilder<ReportWorker>(
                                    report.intervalHours.toLong(),
                                    TimeUnit.HOURS
                            )
                            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf("report_id" to report.id))
                            .build()
                } else {
                    PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS)
                            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf("report_id" to report.id))
                            .build()
                }

        workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        )
    }

    private fun cancelReportWorker(report: Report) {
        WorkManager.getInstance(context).cancelUniqueWork("ReportWorker_${report.id}")
    }
}
