package com.murmli.webpursuer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.murmli.webpursuer.data.AppDatabase
import com.murmli.webpursuer.data.Search
import com.murmli.webpursuer.data.SearchLog
import com.murmli.webpursuer.data.SearchRepository
import com.murmli.webpursuer.worker.SearchWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SearchRepository(database.searchDao(), database.searchLogDao())

    val searches: StateFlow<List<Search>> =
            repository.allSearches.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
            )

    fun getLogsForSearch(searchId: Int): Flow<List<SearchLog>> {
        return repository.getLogsForSearch(searchId)
    }

    fun addSearch(search: Search) {
        viewModelScope.launch {
            val id = repository.addSearch(search)
            if (search.enabled) {
                scheduleWorker(search.copy(id = id.toInt()))
            }
        }
    }

    fun updateSearch(search: Search) {
        viewModelScope.launch {
            repository.updateSearch(search)
            if (search.enabled) {
                scheduleWorker(search)
            } else {
                cancelWorker(search.id)
            }
        }
    }

    fun deleteSearch(search: Search) {
        viewModelScope.launch {
            repository.deleteSearch(search)
            cancelWorker(search.id)
        }
    }

    fun runSearchNow(searchId: Int) {
        val inputData = Data.Builder().putInt("searchId", searchId).build()
        val workRequest =
                OneTimeWorkRequest.Builder(SearchWorker::class.java).setInputData(inputData).build()
        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    private fun scheduleWorker(search: Search) {
        val inputData = Data.Builder().putInt("searchId", search.id).build()
        val uniqueWorkName = "search_${search.id}"
        val workManager = WorkManager.getInstance(getApplication())

        val workRequest =
                if (search.scheduleType == "SPECIFIC_TIME") {
                    val now = Calendar.getInstance()
                    val target =
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, search.scheduleHour)
                                set(Calendar.MINUTE, search.scheduleMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                    if (target.before(now)) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val initialDelay = target.timeInMillis - now.timeInMillis

                    PeriodicWorkRequestBuilder<SearchWorker>(24, TimeUnit.HOURS)
                            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .build()
                } else {
                    // Interval
                    val interval = search.intervalMinutes.coerceAtLeast(15) // Minimum 15 mins
                    PeriodicWorkRequestBuilder<SearchWorker>(interval, TimeUnit.MINUTES)
                            .setInputData(inputData)
                            .build()
                }

        workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE, // Update existing if parameters changed
                workRequest
        )
    }

    private fun cancelWorker(searchId: Int) {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("search_$searchId")
    }
}
