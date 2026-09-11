package com.mindfulscreen.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulscreen.app.MindfulScreenApp
import com.mindfulscreen.app.data.SettingsRepository
import com.mindfulscreen.app.data.UsageStatsRepository
import com.mindfulscreen.app.data.db.AppLimitDao
import com.mindfulscreen.app.data.db.DailyScoreDao
import com.mindfulscreen.app.data.db.DailyScoreEntity
import com.mindfulscreen.app.data.model.AppUsage
import com.mindfulscreen.app.domain.AttentionScore
import com.mindfulscreen.app.domain.AvatarMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val loading: Boolean = true,
    val hasPermission: Boolean = true,
    val score: Int = 100,
    val mood: AvatarMood = AvatarMood.THRIVING,
    val totalMs: Long = 0,
    val goalMs: Long = SettingsRepository.DEFAULT_GOAL_MS,
    val topApps: List<AppUsage> = emptyList(),
    val brokenLimits: List<String> = emptyList(),
)

class DashboardViewModel(
    private val usageRepo: UsageStatsRepository,
    private val settings: SettingsRepository,
    private val appLimitDao: AppLimitDao,
    private val dailyScoreDao: DailyScoreDao,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (!usageRepo.hasUsageAccess()) {
                _state.update { it.copy(loading = false, hasPermission = false) }
                return@launch
            }
            val usage = usageRepo.getDayUsage(LocalDate.now())
            val goalMs = settings.dailyGoalMs.first()
            val limits = appLimitDao.getAll()
            val result = AttentionScore.compute(usage, goalMs, limits)

            // Persist today's score so Insights can chart the trend.
            dailyScoreDao.upsert(
                DailyScoreEntity(
                    epochDay = usage.dateEpochDay,
                    score = result.score,
                    totalMs = usage.totalMs,
                    goalMs = goalMs,
                    goalMet = result.goalMet,
                )
            )

            _state.update {
                it.copy(
                    loading = false,
                    hasPermission = true,
                    score = result.score,
                    mood = AvatarMood.fromScore(result.score),
                    totalMs = usage.totalMs,
                    goalMs = goalMs,
                    topApps = usage.topApps.take(5),
                    brokenLimits = result.brokenLimits,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as MindfulScreenApp)
                DashboardViewModel(
                    app.container.usageStatsRepository,
                    app.container.settingsRepository,
                    app.container.appLimitDao,
                    app.container.dailyScoreDao,
                )
            }
        }
    }
}
