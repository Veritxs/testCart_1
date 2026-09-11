package com.mindfulscreen.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Simple user settings: onboarding completion + daily screen-time goal. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val DAILY_GOAL_MS = longPreferencesKey("daily_goal_ms")
    }

    val onboarded: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    val dailyGoalMs: Flow<Long> =
        context.dataStore.data.map { it[Keys.DAILY_GOAL_MS] ?: DEFAULT_GOAL_MS }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = value }
    }

    suspend fun setDailyGoalMs(value: Long) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_MS] = value }
    }

    companion object {
        /** Default daily goal: 3 hours. */
        const val DEFAULT_GOAL_MS = 3 * 60 * 60 * 1000L
    }
}
