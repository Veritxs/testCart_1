package com.mindfulscreen.app.ui.screens.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulscreen.app.MindfulScreenApp
import com.mindfulscreen.app.data.SettingsRepository
import com.mindfulscreen.app.data.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val hasPermission: Boolean = false,
    val goalMinutes: Int = 180,
)

class OnboardingViewModel(
    private val usageRepo: UsageStatsRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun refreshPermission() {
        _state.update { it.copy(hasPermission = usageRepo.hasUsageAccess()) }
    }

    fun settingsIntent(): Intent =
        usageRepo.usageAccessSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun setGoalMinutes(minutes: Int) {
        _state.update { it.copy(goalMinutes = minutes) }
    }

    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            settings.setDailyGoalMs(_state.value.goalMinutes * 60_000L)
            settings.setOnboarded(true)
            onFinished()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as MindfulScreenApp)
                OnboardingViewModel(
                    app.container.usageStatsRepository,
                    app.container.settingsRepository,
                )
            }
        }
    }
}
