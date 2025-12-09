package com.example.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webpursuer.data.AppDatabase
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.data.CheckLog
import com.example.webpursuer.data.Interaction
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
            val context = getApplication<Application>()
            val settingsRepository = com.example.webpursuer.data.SettingsRepository(context)
            val logRepository = com.example.webpursuer.data.LogRepository(database.appLogDao())
            val openRouterService = com.example.webpursuer.network.OpenRouterService(settingsRepository, logRepository)
            val webChecker = com.example.webpursuer.worker.WebChecker(context, monitorDao, checkLogDao, interactionDao, openRouterService, settingsRepository, logRepository)
            webChecker.checkMonitor(monitor, System.currentTimeMillis())
        }
    }

    suspend fun getMonitor(id: Int): Monitor? {
        return monitorDao.getById(id)
    }

    suspend fun getInteractions(monitorId: Int): List<Interaction> {
        return interactionDao.getInteractionsForMonitor(monitorId)
    }

    suspend fun getCheckLog(id: Int): CheckLog? {
        return checkLogDao.getById(id)
    }

    suspend fun getPreviousCheckLog(monitorId: Int, currentTimestamp: Long): CheckLog? {
        return checkLogDao.getPreviousLog(monitorId, currentTimestamp)
    }
}
