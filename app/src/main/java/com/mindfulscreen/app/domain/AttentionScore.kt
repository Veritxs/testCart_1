package com.mindfulscreen.app.domain

import com.mindfulscreen.app.data.db.AppLimitEntity
import com.mindfulscreen.app.data.model.DayUsage
import kotlin.math.roundToInt

/**
 * Computes the daily Attention Score (0–100).
 *
 * Design rationale (see DESIGN.md §3): grounded in feedback-loop + loss-aversion
 * psychology. Everyone starts each day at 100 and loses points for exceeding the
 * daily goal and per-app limits. A single, legible number that only goes down when
 * you overuse is a stronger motivator than a raw minutes chart.
 */
object AttentionScore {

    /** Points lost per hour over the daily goal. */
    private const val PENALTY_PER_HOUR_OVER = 20.0

    /** Points lost for each exceeded per-app limit. */
    private const val PENALTY_PER_BROKEN_LIMIT = 8.0

    /** Points lost per hour of late-night (00:00–06:00) usage. */
    private const val PENALTY_PER_LATE_NIGHT_HOUR = 10.0

    data class Result(
        val score: Int,
        val goalMet: Boolean,
        val brokenLimits: List<String>,
    )

    fun compute(
        usage: DayUsage,
        dailyGoalMs: Long,
        limits: List<AppLimitEntity>,
    ): Result {
        var score = 100.0

        // Penalty for exceeding the overall daily goal.
        val overMs = (usage.totalMs - dailyGoalMs).coerceAtLeast(0)
        score -= (overMs / 3_600_000.0) * PENALTY_PER_HOUR_OVER

        // Penalty for each per-app limit broken.
        val usageByPkg = usage.perApp.associateBy { it.packageName }
        val broken = limits.filter { limit ->
            (usageByPkg[limit.packageName]?.timeMs ?: 0L) > limit.limitMs
        }
        score -= broken.size * PENALTY_PER_BROKEN_LIMIT

        // Penalty for late-night usage.
        score -= (usage.lateNightMs / 3_600_000.0) * PENALTY_PER_LATE_NIGHT_HOUR

        val clamped = score.coerceIn(0.0, 100.0).roundToInt()
        return Result(
            score = clamped,
            goalMet = usage.totalMs <= dailyGoalMs,
            brokenLimits = broken.map { it.label },
        )
    }
}

/** Avatar mood derived from the score — the emotional feedback hook. */
enum class AvatarMood(val emoji: String, val caption: String) {
    THRIVING("\uD83E\uDD29", "Thriving! Great focus today."),
    GOOD("\uD83D\uDE42", "Doing well — keep it mindful."),
    TIRED("\uD83D\uDE2A", "Getting tired. Take a break?"),
    MELTING("\uD83E\uDEE0", "Melting… time to put the phone down.");

    companion object {
        fun fromScore(score: Int): AvatarMood = when {
            score >= 85 -> THRIVING
            score >= 65 -> GOOD
            score >= 40 -> TIRED
            else -> MELTING
        }
    }
}
