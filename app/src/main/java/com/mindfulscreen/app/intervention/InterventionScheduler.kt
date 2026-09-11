package com.mindfulscreen.app.intervention

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the periodic limit-check worker (min interval on Android is 15 minutes). */
object InterventionScheduler {

    private const val WORK_NAME = "limit_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<LimitCheckWorker>(
            repeatInterval = 15, repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
