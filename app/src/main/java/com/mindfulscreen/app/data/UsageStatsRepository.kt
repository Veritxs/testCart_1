package com.mindfulscreen.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.mindfulscreen.app.data.model.AppUsage
import com.mindfulscreen.app.data.model.DayUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads real per-app usage from Android's [UsageStatsManager].
 *
 * Requires the special "Usage Access" permission (PACKAGE_USAGE_STATS), which the
 * user grants in system Settings — see [hasUsageAccess] / [usageAccessSettingsIntent].
 *
 * Design note (accuracy): we use [UsageStatsManager.queryAndAggregateUsageStats] as the
 * single source of truth for per-app time. It merges the OS usage buckets for the day
 * and keys them by package — the same underlying data the system "Digital Wellbeing"
 * screen shows. Earlier versions reconstructed time from raw foreground/background
 * events, but on some OEM builds (notably Samsung/One UI) those events are batched or
 * dropped, which produced both phantom usage and under-counting. The aggregate API is
 * far more reliable across devices.
 */
class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    /** True if the user has granted Usage Access to this app. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Intent to open the system Usage Access settings screen. */
    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Aggregated usage for [date] (defaults to today). */
    suspend fun getDayUsage(date: LocalDate = LocalDate.now()): DayUsage =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val end = minOf(endOfDay, now)

            val perApp = queryAggregatedUsage(start, end)
            val total = perApp.sumOf { it.timeMs }

            DayUsage(
                dateEpochDay = date.toEpochDay(),
                totalMs = total,
                perApp = perApp.sortedByDescending { it.timeMs },
                lateNightMs = queryLateNightMs(start, end, zone),
            )
        }

    /**
     * Per-app foreground time for [start,end] from the OS aggregate.
     *
     * We report the larger of [android.app.usage.UsageStats.getTotalTimeInForeground]
     * and, where available (API 29+), the "visible" time — Digital Wellbeing counts the
     * time an app is visible on screen, which for some apps exceeds strict foreground
     * time. Taking the max brings our numbers in line with the system figure.
     */
    private fun queryAggregatedUsage(start: Long, end: Long): List<AppUsage> {
        val map = usageStatsManager.queryAndAggregateUsageStats(start, end)
        if (map.isEmpty()) return emptyList()

        val myPackage = context.packageName
        return map.values.mapNotNull { stats ->
            var ms = stats.totalTimeInForeground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ms = maxOf(ms, stats.totalTimeVisible)
            }
            val pkg = stats.packageName
            when {
                ms <= 0L -> null
                pkg == myPackage -> null            // don't report ourselves
                !isLaunchable(pkg) -> null          // skip system/background-only packages
                else -> AppUsage(pkg, labelFor(pkg), ms, opens = 0)
            }
        }
    }

    /**
     * Milliseconds of usage that fell between 00:00–06:00 for the day, derived from
     * events. This is a supplementary signal only (a highlight on the Insights screen),
     * so any small event inaccuracy here never affects the per-app totals above.
     */
    private fun queryLateNightMs(start: Long, end: Long, zone: ZoneId): Long {
        val nightEnd = minOf(
            end,
            LocalDate.now(zone).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        )
        if (nightEnd <= start) return 0L

        val events = usageStatsManager.queryEvents(start, nightEnd)
        val event = android.app.usage.UsageEvents.Event()
        var currentSince = -1L
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (currentSince < 0) currentSince = event.timeStamp
                }
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (currentSince >= 0) {
                        total += (event.timeStamp - currentSince).coerceIn(0, MAX_SESSION_MS)
                        currentSince = -1L
                    }
                }
            }
        }
        if (currentSince >= 0) {
            total += (nightEnd - currentSince).coerceIn(0, MAX_SESSION_MS)
        }
        return total
    }

    /** True if the package has a launcher entry (i.e. a user-facing app). */
    private fun isLaunchable(pkg: String): Boolean =
        launchableCache.getOrPut(pkg) {
            packageManager.getLaunchIntentForPackage(pkg) != null
        }

    private val launchableCache = HashMap<String, Boolean>()

    private val labelCache = HashMap<String, String>()
    private fun labelFor(pkg: String): String = labelCache.getOrPut(pkg) {
        try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg.substringAfterLast('.')
        }
    }

    companion object {
        /** Cap on a single late-night session, guarding against a dropped stop event. */
        private const val MAX_SESSION_MS = 2 * 60 * 60 * 1000L
    }
}
