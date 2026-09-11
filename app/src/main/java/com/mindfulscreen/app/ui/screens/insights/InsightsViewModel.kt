package com.mindfulscreen.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulscreen.app.MindfulScreenApp
import com.mindfulscreen.app.data.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayBar(val date: LocalDate, val totalMs: Long)

data class InsightsState(
    val loading: Boolean = true,
    val hasPermission: Boolean = true,
    val week: List<DayBar> = emptyList(),
    val thisWeekMs: Long = 0,
    val lastWeekMs: Long = 0,
    val mostUsedLabel: String? = null,
    val mostUsedMs: Long = 0,
    val lateNightMs: Long = 0,
) {
    /** Signed percentage change vs last week (negative = improvement). */
    val weekOverWeekPct: Int?
        get() = if (lastWeekMs == 0L) null
        else (((thisWeekMs - lastWeekMs).toDouble() / lastWeekMs) * 100).toInt()
}

class InsightsViewModel(
    private val usageRepo: UsageStatsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (!usageRepo.hasUsageAccess()) {
                _state.update { it.copy(loading = false, hasPermission = false) }
                return@launch
            }
            val today = LocalDate.now()

            // Last 7 days for the chart.
            val week = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                DayBar(date, usageRepo.getDayUsage(date).totalMs)
            }

            // Previous 7 days for the comparison.
            val prevWeekTotal = (13 downTo 7).sumOf { offset ->
                usageRepo.getDayUsage(today.minusDays(offset.toLong())).totalMs
            }
            val thisWeekTotal = week.sumOf { it.totalMs }

            // Most-used app + late-night usage across this week.
            val agg = HashMap<String, Pair<String, Long>>()
            var lateNight = 0L
            (6 downTo 0).forEach { offset ->
                val day = usageRepo.getDayUsage(today.minusDays(offset.toLong()))
                lateNight += day.lateNightMs
                day.perApp.forEach { a ->
                    val cur = agg[a.packageName]
                    agg[a.packageName] = a.label to ((cur?.second ?: 0L) + a.timeMs)
                }
            }
            val mostUsed = agg.values.maxByOrNull { it.second }

            _state.update {
                it.copy(
                    loading = false,
                    hasPermission = true,
                    week = week,
                    thisWeekMs = thisWeekTotal,
                    lastWeekMs = prevWeekTotal,
                    mostUsedLabel = mostUsed?.first,
                    mostUsedMs = mostUsed?.second ?: 0,
                    lateNightMs = lateNight,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as MindfulScreenApp)
                InsightsViewModel(app.container.usageStatsRepository)
            }
        }
    }
}
