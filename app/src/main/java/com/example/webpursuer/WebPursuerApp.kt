package com.example.webpursuer

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.webpursuer.worker.WebCheckWorker
import java.util.concurrent.TimeUnit

class WebPursuerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupWorker()
    }

    private fun setupWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WebCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WebCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
