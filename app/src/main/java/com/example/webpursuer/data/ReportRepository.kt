package com.example.webpursuer.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.webpursuer.worker.ReportWorker
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
        val workManager = WorkManager.getInstance(context) // ReportWorker needs to be adjusted!
        val workName = "ReportWorker_${report.id}"

        if (!report.enabled) {
            workManager.cancelUniqueWork(workName)
            return
        }

        // Calculate initial delay
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, report.scheduleHour)
            set(Calendar.MINUTE, report.scheduleMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("report_id" to report.id))
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelReportWorker(report: Report) {
        WorkManager.getInstance(context).cancelUniqueWork("ReportWorker_${report.id}")
    }
}
