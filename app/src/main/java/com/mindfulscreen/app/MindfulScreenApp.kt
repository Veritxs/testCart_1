package com.mindfulscreen.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mindfulscreen.app.data.AppContainer

/**
 * Application class. Holds the [AppContainer] (a lightweight manual DI container)
 * so ViewModels can reach repositories without a DI framework.
 */
class MindfulScreenApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        // Start periodic limit checks that drive the mindful nudges.
        com.mindfulscreen.app.intervention.InterventionScheduler.schedule(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NUDGE_CHANNEL_ID,
                "Mindful Nudges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle reminders when you exceed a screen-time limit"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val NUDGE_CHANNEL_ID = "mindful_nudges"
    }
}

/** Convenience accessor for the container from any Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as MindfulScreenApp).container
