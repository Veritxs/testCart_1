package com.mindfulscreen.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A per-app daily limit set by the user (e.g. TikTok = 30 min). */
@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val limitMs: Long,
)

/** The computed Attention Score for a given day (0–100). */
@Entity(tableName = "daily_scores")
data class DailyScoreEntity(
    @PrimaryKey val epochDay: Long,
    val score: Int,
    val totalMs: Long,
    val goalMs: Long,
    val goalMet: Boolean,
)
