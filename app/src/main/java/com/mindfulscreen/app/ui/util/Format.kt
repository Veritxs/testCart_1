package com.mindfulscreen.app.ui.util

/** Formats a millisecond duration as a compact human string, e.g. "2h 14m" or "37m". */
fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

/** Formats minutes as "Xh Ym" for goal displays. */
fun formatMinutes(min: Long): String = formatDuration(min * 60000)
