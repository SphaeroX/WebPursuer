package com.example.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.Monitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val monitorDao = database.monitorDao()

    val monitors: StateFlow<List<Monitor>> = monitorDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMonitor(monitor: Monitor) {
        viewModelScope.launch {
            monitorDao.insert(monitor)
        }
    }

    fun deleteMonitor(monitor: Monitor) {
        viewModelScope.launch {
            monitorDao.delete(monitor)
        }
    }
}
