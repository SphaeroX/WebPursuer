package com.example.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.Report
import com.example.webpursuer.data.ReportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReportRepository

    val reports: StateFlow<List<Report>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ReportRepository(application, database.reportDao())
        reports =
                repository.allReports.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        emptyList()
                )
    }

    private val settingsRepository by lazy {
        com.example.webpursuer.data.SettingsRepository(application)
    }

    val apiKey: kotlinx.coroutines.flow.Flow<String?> = settingsRepository.apiKey

    fun addReport(
            name: String,
            customPrompt: String,
            scheduleType: String,
            scheduleHour: Int,
            scheduleMinute: Int,
            scheduleDays: Int, // Bitmask
            intervalHours: Int,
            monitorIds: Set<Int>,
            useWebSearch: Boolean = false
    ) {
        viewModelScope.launch {
            val monitorIdsStr = monitorIds.joinToString(",")
            val report =
                    Report(
                            name = name,
                            customPrompt = customPrompt,
                            scheduleType = scheduleType,
                            scheduleHour = scheduleHour,
                            scheduleMinute = scheduleMinute,
                            scheduleDays = scheduleDays,
                            intervalHours = intervalHours,
                            monitorIds = monitorIdsStr,
                            enabled = true,
                            useWebSearch = useWebSearch
                    )
            repository.insertReport(report)
        }
    }

    fun updateReport(report: Report, monitorIds: Set<Int>) {
        viewModelScope.launch {
            val monitorIdsStr = monitorIds.joinToString(",")
            val updatedReport = report.copy(monitorIds = monitorIdsStr)
            repository.updateReport(updatedReport)
        }
    }

    fun updateReportFull(report: Report) {
        viewModelScope.launch { repository.updateReport(report) }
    }

    fun toggleReport(report: Report, enabled: Boolean) {
        viewModelScope.launch { repository.updateReport(report.copy(enabled = enabled)) }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch { repository.deleteReport(report) }
    }
}
