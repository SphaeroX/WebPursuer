package com.example.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.data.CheckLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val monitorDao = database.monitorDao()
    private val checkLogDao = database.checkLogDao()

    val monitors: StateFlow<List<Monitor>> = monitorDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getLogsForMonitor(monitorId: Int): Flow<List<CheckLog>> {
        return checkLogDao.getLogsForMonitor(monitorId)
    }

    fun addMonitor(monitor: Monitor) {
        viewModelScope.launch {
            monitorDao.insert(monitor)
        }
    }

    fun updateMonitor(monitor: Monitor) {
        viewModelScope.launch {
            monitorDao.update(monitor)
        }
    }

    fun deleteMonitor(monitor: Monitor) {
        viewModelScope.launch {
            monitorDao.delete(monitor)
        }
    }
    
    fun checkNow(monitor: Monitor) {
        viewModelScope.launch {
            // Simulate check for now
            val result = if (Math.random() > 0.5) "CHANGED" else "UNCHANGED"
            val message = if (result == "CHANGED") "Content changed!" else "No changes detected."
            
            checkLogDao.insert(
                CheckLog(
                    monitorId = monitor.id,
                    timestamp = System.currentTimeMillis(),
                    result = result,
                    message = message
                )
            )
        }
    }
}
