package com.mindfulscreen.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.mindfulscreen.app.data.model.AppUsage
import com.mindfulscreen.app.data.model.DayUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

/**
 * Reads real per-app usage from Android's [UsageStatsManager].
 *
 * Requires the special "Usage Access" permission (PACKAGE_USAGE_STATS), which the
 * user grants in system Settings — see [hasUsageAccess] / [usageAccessSettingsIntent].
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

    /** Aggregated usage for [date] (defaults to today), via event replay for accuracy. */
    suspend fun getDayUsage(date: LocalDate = LocalDate.now()): DayUsage =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val end = minOf(endOfDay, now)

            val perApp = queryForegroundTime(start, end, zone)
            val total = perApp.values.sumOf { it.timeMs }
            val lateNight = perApp.values.sumOf { it.lateNightMs }

            DayUsage(
                dateEpochDay = date.toEpochDay(),
                totalMs = total,
                perApp = perApp.values
                    .filter { it.timeMs > 0 }
                    .map { AppUsage(it.pkg, labelFor(it.pkg), it.timeMs, it.opens) },
                lateNightMs = lateNight,
            )
        }

    /**
     * Reconstructs per-app foreground time by replaying MOVE_TO_FOREGROUND /
     * MOVE_TO_BACKGROUND events. More accurate than queryUsageStats aggregates,
     * which can double-count across buckets.
     */
    private fun queryForegroundTime(
        start: Long,
        end: Long,
        zone: ZoneId,
    ): Map<String, MutableAppAccum> {
        val result = HashMap<String, MutableAppAccum>()
        val events = usageStatsManager.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()
        // Track the last foreground timestamp per package.
        val foregroundSince = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND,
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundSince[pkg] = event.timeStamp
                    result.getOrPut(pkg) { MutableAppAccum(pkg) }.opens++
                }
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND,
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val since = foregroundSince.remove(pkg) ?: continue
                    accumulate(result, pkg, since, event.timeStamp, zone)
                }
            }
        }
        // Any app still in foreground at query end.
        for ((pkg, since) in foregroundSince) {
            accumulate(result, pkg, since, end, zone)
        }
        return result
    }

    private fun accumulate(
        map: MutableMap<String, MutableAppAccum>,
        pkg: String,
        from: Long,
        to: Long,
        zone: ZoneId,
    ) {
        if (to <= from) return
        val accum = map.getOrPut(pkg) { MutableAppAccum(pkg) }
        accum.timeMs += (to - from)
        accum.lateNightMs += lateNightOverlap(from, to, zone)
    }

    /** Milliseconds of [from,to] that fall between 00:00–06:00 local (late-night usage). */
    private fun lateNightOverlap(from: Long, to: Long, zone: ZoneId): Long {
        val day = LocalDateTime.ofEpochSecond(from / 1000, 0,
            zone.rules.getOffset(java.time.Instant.ofEpochMilli(from))).toLocalDate()
        val nightStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val nightEnd = day.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        val lo = maxOf(from, nightStart)
        val hi = minOf(to, nightEnd)
        return if (hi > lo) hi - lo else 0L
    }

    private val labelCache = HashMap<String, String>()
    private fun labelFor(pkg: String): String = labelCache.getOrPut(pkg) {
        try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg.substringAfterLast('.')
        }
    }

    private class MutableAppAccum(val pkg: String) {
        var timeMs: Long = 0
        var opens: Int = 0
        var lateNightMs: Long = 0
    }
}
