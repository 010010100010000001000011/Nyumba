package com.j01000011.nyumba

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.j01000011.nyumba.worker.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NyumbaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupReminderWorker()
        triggerImmediateReminderCheck()
    }

    private fun setupReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            12, TimeUnit.HOURS 
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NyumbaPeriodicReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Requirement 16: "re-check missed reminder windows when the app becomes active"
     * Triggers a one-time immediate execution of the reminder engine.
     */
    private fun triggerImmediateReminderCheck() {
        val immediateRequest = OneTimeWorkRequestBuilder<ReminderWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "NyumbaImmediateReminderCheck",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }
}
