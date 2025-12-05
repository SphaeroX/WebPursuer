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
        reports = repository.allReports
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addReport(name: String, customPrompt: String, hour: Int, minute: Int, monitorIds: Set<Int>) {
        viewModelScope.launch {
            val monitorIdsStr = monitorIds.joinToString(",")
            val report = Report(
                name = name,
                customPrompt = customPrompt,
                scheduleHour = hour,
                scheduleMinute = minute,
                monitorIds = monitorIdsStr,
                enabled = true
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
    
    fun toggleReport(report: Report, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateReport(report.copy(enabled = enabled))
        }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch {
            repository.deleteReport(report)
        }
    }
}
