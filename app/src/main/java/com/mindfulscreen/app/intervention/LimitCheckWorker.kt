package com.mindfulscreen.app.intervention

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindfulscreen.app.MindfulScreenApp
import com.mindfulscreen.app.R
import com.mindfulscreen.app.appContainer
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Periodically compares today's real usage against the user's limits and, when a
 * limit is freshly exceeded, fires a full-screen mindful nudge. To avoid nagging,
 * each limit only nudges once per day.
 */
class LimitCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val usageRepo = container.usageStatsRepository
        if (!usageRepo.hasUsageAccess()) return Result.success()

        val usage = usageRepo.getDayUsage(LocalDate.now())
        val limits = container.appLimitDao.getAll()
        val goalMs = container.settingsRepository.dailyGoalMs.first()
        val usageByPkg = usage.perApp.associateBy { it.packageName }

        // Per-app limits.
        for (limit in limits) {
            val used = usageByPkg[limit.packageName]?.timeMs ?: 0L
            if (used > limit.limitMs && shouldNotifyToday(limit.packageName)) {
                nudge(limit.label, (used / 60_000L).toInt())
                markNotifiedToday(limit.packageName)
            }
        }

        // Overall daily goal.
        if (usage.totalMs > goalMs && shouldNotifyToday(DAILY_KEY)) {
            nudge("your phone", (usage.totalMs / 60_000L).toInt())
            markNotifiedToday(DAILY_KEY)
        }

        return Result.success()
    }

    private fun nudge(appLabel: String, minutes: Int) {
        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val fullScreenIntent = Intent(applicationContext, NudgeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(NudgeActivity.EXTRA_APP_LABEL, appLabel)
            putExtra(NudgeActivity.EXTRA_MINUTES, minutes)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            appLabel.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            applicationContext, MindfulScreenApp.NUDGE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Mindful moment")
            .setContentText("You've passed your limit on $appLabel ($minutes min). Take a breath?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()

        val granted = ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        // POST_NOTIFICATIONS is only required on API 33+; older versions are always allowed.
        if (granted || android.os.Build.VERSION.SDK_INT < 33) {
            nm.notify(appLabel.hashCode(), notification)
        }
    }

    // --- Once-per-day de-dupe, stored in SharedPreferences ---

    private val prefs by lazy {
        applicationContext.getSharedPreferences("nudge_state", Context.MODE_PRIVATE)
    }

    private fun shouldNotifyToday(key: String): Boolean {
        val today = LocalDate.now().toEpochDay()
        return prefs.getLong("notified_$key", -1L) != today
    }

    private fun markNotifiedToday(key: String) {
        prefs.edit().putLong("notified_$key", LocalDate.now().toEpochDay()).apply()
    }

    companion object {
        private const val DAILY_KEY = "__daily_goal__"
    }
}
