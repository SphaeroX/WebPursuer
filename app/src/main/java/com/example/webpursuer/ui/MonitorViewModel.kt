package com.example.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.data.CheckLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val monitorDao = database.monitorDao()
    private val checkLogDao = database.checkLogDao()
    private val interactionDao = database.interactionDao()

    val monitors: StateFlow<List<Monitor>> = monitorDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getLogsForMonitor(monitorId: Int): Flow<List<CheckLog>> {
        return checkLogDao.getLogsForMonitor(monitorId)
    }

    fun addMonitor(monitor: Monitor, interactions: List<com.example.webpursuer.data.Interaction>) {
        viewModelScope.launch {
            monitorDao.insert(monitor)
            // We need the ID of the inserted monitor. 
            // Since Room's insert returns Long (rowId), we should change DAO to return Long.
            // But for now, let's assume we can get the last inserted ID or change DAO.
            // A better way is to let insert return Long.
            val insertedId = monitorDao.insertAndReturnId(monitor)
            
            val interactionsWithId = interactions.map { it.copy(monitorId = insertedId.toInt()) }
            interactionDao.insertAll(interactionsWithId)
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
        viewModelScope.launch(Dispatchers.IO) {
            val webChecker = com.example.webpursuer.worker.WebChecker(getApplication(), monitorDao, checkLogDao, interactionDao)
            webChecker.checkMonitor(monitor, System.currentTimeMillis())
        }
    }
}
