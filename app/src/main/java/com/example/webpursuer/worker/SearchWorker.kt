package com.murmli.webpursuer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.murmli.webpursuer.data.AppDatabase
import com.murmli.webpursuer.data.LogRepository
import com.murmli.webpursuer.data.SearchRepository
import com.murmli.webpursuer.data.SettingsRepository
import com.murmli.webpursuer.network.OpenRouterService
import kotlinx.coroutines.flow.first

class SearchWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val searchId = inputData.getInt("searchId", -1)
        if (searchId == -1) return Result.failure()

        val database = AppDatabase.getDatabase(applicationContext)
        val searchRepository = SearchRepository(database.searchDao(), database.searchLogDao())
        val settingsRepository = SettingsRepository(applicationContext)
        val logRepository = LogRepository(database.appLogDao())
        val openRouterService = OpenRouterService(settingsRepository, logRepository)

        return try {
            val search = searchRepository.getSearchById(searchId)
            if (search == null) {
                logRepository.logError("SearchWorker", "Search ID $searchId not found")
                return Result.failure()
            }

            logRepository.logInfo("SearchWorker", "Executing search: ${search.prompt}")

            // Check if today is allowed for SPECIFIC_TIME
            if (search.scheduleType == "SPECIFIC_TIME") {
                val calendar = java.util.Calendar.getInstance()
                // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to our index (Mon=0..Sun=6)
                // Mon(2) -> 0 => (2+5)%7 = 0
                // Sun(1) -> 6 => (1+5)%7 = 6
                val dayIndex = (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                val isTodayEnabled = (search.scheduleDays and (1 shl dayIndex)) != 0

                if (!isTodayEnabled) {
                    logRepository.logInfo(
                            "SearchWorker",
                            "Today is not enabled in schedule. Skipping."
                    )
                    return Result.success()
                }
            }

            // 1. Perform the search / get completion
            // passing useWebSearch = true to use the web plugin
            val response = openRouterService.performSearch(search.prompt, useWebSearch = true)

            var aiConditionMet: Boolean? = null
            var finalResult = response

            // 2. Check AI Condition if enabled
            if (search.aiConditionEnabled && search.aiConditionPrompt != null) {
                // We reuse checkContent from OpenRouter, but passing the Search Result as the
                // "Content"
                // The checkContent logic asks: "User Condition: $prompt\n\nWeb Content:\n$content"
                // So we pass condition as prompt, and search result as content.
                aiConditionMet =
                        openRouterService.checkContent(
                                prompt = search.aiConditionPrompt,
                                content = response,
                                useWebSearch = false // No need to search web for evaluation
                        )

                // If condition enabled and not met, we might want to flag it in the log
                if (search.notificationEnabled && aiConditionMet == true) {
                    sendNotification(
                            searchId = search.id,
                            title = "Search Alert",
                            message = "Condition met for search: ${search.prompt}"
                    )
                    logRepository.logInfo("SearchWorker", "Condition MET, triggering notification.")
                }
            } else {
                if (search.notificationEnabled) {
                    sendNotification(
                            searchId = search.id,
                            title = "Search Result",
                            message = "New search result for: ${search.prompt}"
                    )
                }
            }

            searchRepository.logResult(searchId, finalResult, aiConditionMet)

            // Update last run time
            searchRepository.updateSearch(search.copy(lastRunTime = System.currentTimeMillis()))

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Use runBlocking or similar if needing repositories, but here we can just log if we
            // had a proper logger available globally
            // or re-instantiate.
            Result.retry()
        }
    }

    private suspend fun sendNotification(searchId: Int, title: String, message: String) {
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        // Check if notifications are enabled globally
        val isGloballyEnabled = settingsRepository.notificationsEnabled.first()
        if (!isGloballyEnabled) {
            return
        }

        val intent =
                android.content.Intent(
                                applicationContext,
                                com.murmli.webpursuer.MainActivity::class.java
                        )
                        .apply {
                            flags =
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            // We might want to pass searchId here to navigate relevantly later
                            // putExtra("searchId", searchId)
                        }
        val pendingIntent: android.app.PendingIntent =
                android.app.PendingIntent.getActivity(
                        applicationContext,
                        searchId, // unique per search
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

        // Using same channel as Monitors for now "web_monitor_channel" or "search_channel" if
        // created.
        // Assuming "web_monitor_channel" is the main one created in App.
        val builder =
                androidx.core.app.NotificationCompat.Builder(
                                applicationContext,
                                "web_monitor_channel"
                        )
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)

        try {
            with(androidx.core.app.NotificationManagerCompat.from(applicationContext)) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                applicationContext,
                                android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notify(
                            searchId + 10000,
                            builder.build()
                    ) // Offset ID to avoid collision with Monitors
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
