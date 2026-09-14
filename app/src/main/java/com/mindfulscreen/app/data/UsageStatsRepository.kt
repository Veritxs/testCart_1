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

    /**
     * Aggregated usage for [date] (defaults to today).
     *
     * Per-app foreground time comes from Android's authoritative
     * [UsageStatsManager.queryUsageStats] with INTERVAL_DAILY — the same source the
     * system "Digital Wellbeing" screen uses, so our numbers match the phone's own.
     * Event replay is used only for supplementary signals the aggregate can't give us:
     * open counts and the late-night (00:00–06:00) portion.
     */
    suspend fun getDayUsage(date: LocalDate = LocalDate.now()): DayUsage =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val end = minOf(endOfDay, now)

            // Authoritative per-app foreground time from the OS.
            val foregroundByPkg = queryDailyForegroundMs(start, end)
            // Supplementary signals (opens + late-night split) from event replay.
            val replay = queryForegroundTime(start, end, zone)

            val perApp = foregroundByPkg
                .filter { (_, ms) -> ms > 0 }
                .map { (pkg, ms) ->
                    AppUsage(
                        packageName = pkg,
                        label = labelFor(pkg),
                        timeMs = ms,
                        opens = replay[pkg]?.opens ?: 0,
                    )
                }
            val total = perApp.sumOf { it.timeMs }
            val lateNight = replay.values.sumOf { it.lateNightMs }

            DayUsage(
                dateEpochDay = date.toEpochDay(),
                totalMs = total,
                perApp = perApp,
                lateNightMs = lateNight,
            )
        }

    /**
     * Per-app foreground milliseconds for [start,end] using the OS daily aggregates.
     * Because INTERVAL_DAILY buckets can overlap the requested window, we take the max
     * reported foreground time per package rather than summing (summing double-counts).
     */
    private fun queryDailyForegroundMs(start: Long, end: Long): Map<String, Long> {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, start, end
        ) ?: return emptyMap()
        val result = HashMap<String, Long>()
        for (s in stats) {
            val fg = s.totalTimeInForeground
            if (fg <= 0) continue
            val prev = result[s.packageName] ?: 0L
            if (fg > prev) result[s.packageName] = fg
        }
        return result
    }

    /**
     * Reconstructs per-app foreground time by replaying usage events.
     *
     * Key correctness rules (fixing earlier over-counting):
     *  - Only ONE app is in the foreground at a time. When a new app comes to the
     *    foreground, the previously-foreground app's session is closed. We never rely
     *    solely on per-app resume/pause pairing, because those events are frequently
     *    dropped and leave sessions open for hours.
     *  - We prefer the modern ACTIVITY_RESUMED/PAUSED events and ignore the legacy
     *    MOVE_TO_FOREGROUND/BACKGROUND duplicates so a single session isn't double-opened.
     *  - Any single session is capped (a dropped "stop" event can't inflate to hours).
     */
    private fun queryForegroundTime(
        start: Long,
        end: Long,
        zone: ZoneId,
    ): Map<String, MutableAppAccum> {
        val result = HashMap<String, MutableAppAccum>()
        val events = usageStatsManager.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()

        // The single app currently in the foreground, and since when.
        var currentPkg: String? = null
        var currentSince: Long = 0L

        fun closeCurrent(at: Long) {
            val pkg = currentPkg ?: return
            accumulate(result, pkg, currentSince, at, zone)
            currentPkg = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                // App comes to the foreground: close whoever was there, open this one.
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != currentPkg) {
                        // Cap the outgoing session in case its stop event was dropped.
                        closeCurrent(event.timeStamp)
                        currentPkg = pkg
                        currentSince = event.timeStamp
                        result.getOrPut(pkg) { MutableAppAccum(pkg) }.opens++
                    }
                }
                // This app left the foreground: close its session if it was the active one.
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (pkg == currentPkg) closeCurrent(event.timeStamp)
                }
                // Screen turned off / became non-interactive: nothing is in the foreground.
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    closeCurrent(event.timeStamp)
                }
            }
        }
        // Close whatever was still open (accumulate() caps runaway sessions).
        currentPkg?.let { accumulate(result, it, currentSince, end, zone) }
        return result
    }

    private fun accumulate(
        map: MutableMap<String, MutableAppAccum>,
        pkg: String,
        from: Long,
        rawTo: Long,
        zone: ZoneId,
    ) {
        if (rawTo <= from) return
        // Guard against dropped "stop" events inflating a single session to hours.
        val to = minOf(rawTo, from + MAX_SESSION_MS)
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

    companion object {
        /**
         * Maximum credited length of a single uninterrupted foreground session.
         * A dropped "stop"/screen-off event can otherwise leave a session open for
         * hours; capping it prevents phantom usage like "Gojek 4h" from a stray event.
         * 30 minutes comfortably covers a normal continuous session.
         */
        private const val MAX_SESSION_MS = 30 * 60 * 1000L
    }
}
