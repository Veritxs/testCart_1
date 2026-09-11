package com.mindfulscreen.app.ui.screens.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulscreen.app.MindfulScreenApp
import com.mindfulscreen.app.data.SettingsRepository
import com.mindfulscreen.app.data.UsageStatsRepository
import com.mindfulscreen.app.data.db.AppLimitDao
import com.mindfulscreen.app.data.db.AppLimitEntity
import com.mindfulscreen.app.data.model.AppUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LimitsState(
    val loading: Boolean = true,
    val goalMinutes: Int = 180,
    val limits: List<AppLimitEntity> = emptyList(),
    val candidateApps: List<AppUsage> = emptyList(),
)

class LimitsViewModel(
    private val usageRepo: UsageStatsRepository,
    private val settings: SettingsRepository,
    private val appLimitDao: AppLimitDao,
) : ViewModel() {

    private val _state = MutableStateFlow(LimitsState())
    val state: StateFlow<LimitsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val goalMs = settings.dailyGoalMs.first()
            val limits = appLimitDao.getAll()
            val candidates = if (usageRepo.hasUsageAccess())
                usageRepo.getDayUsage(LocalDate.now()).topApps.take(12)
            else emptyList()
            _state.update {
                it.copy(
                    loading = false,
                    goalMinutes = (goalMs / 60_000L).toInt(),
                    limits = limits,
                    candidateApps = candidates,
                )
            }
        }
    }

    fun setGoalMinutes(minutes: Int) {
        _state.update { it.copy(goalMinutes = minutes) }
        viewModelScope.launch { settings.setDailyGoalMs(minutes * 60_000L) }
    }

    fun setLimit(app: AppUsage, minutes: Int) {
        viewModelScope.launch {
            appLimitDao.upsert(
                AppLimitEntity(app.packageName, app.label, minutes * 60_000L)
            )
            reloadLimits()
        }
    }

    fun removeLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            appLimitDao.delete(limit)
            reloadLimits()
        }
    }

    private suspend fun reloadLimits() {
        _state.update { it.copy(limits = appLimitDao.getAll()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as MindfulScreenApp)
                LimitsViewModel(
                    app.container.usageStatsRepository,
                    app.container.settingsRepository,
                    app.container.appLimitDao,
                )
            }
        }
    }
}
