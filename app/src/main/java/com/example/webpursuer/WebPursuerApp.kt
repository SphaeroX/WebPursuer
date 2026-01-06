package com.murmli.webpursuer

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.murmli.webpursuer.worker.WebCheckWorker
import java.util.concurrent.TimeUnit

class WebPursuerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val constraints =
                androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()

        val workRequest =
                PeriodicWorkRequestBuilder<WebCheckWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "WebCheckWork",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                )
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Web Monitor Updates"
            val descriptionText = "Notifications for web page changes"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                    android.app.NotificationChannel("web_monitor_channel", name, importance).apply {
                        description = descriptionText
                    }
            val notificationManager: android.app.NotificationManager =
                    getSystemService(android.content.Context.NOTIFICATION_SERVICE) as
                            android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
