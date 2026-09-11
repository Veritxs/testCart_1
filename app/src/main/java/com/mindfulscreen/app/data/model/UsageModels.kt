package com.mindfulscreen.app.data.model

/** One app's usage for a day. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val timeMs: Long,
    val opens: Int,
)

/** Aggregated usage for a single day. */
data class DayUsage(
    val dateEpochDay: Long,
    val totalMs: Long,
    val perApp: List<AppUsage>,
    val lateNightMs: Long = 0L,
) {
    val topApps: List<AppUsage> get() = perApp.sortedByDescending { it.timeMs }
}
