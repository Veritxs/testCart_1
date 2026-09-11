package com.mindfulscreen.app.data

import android.content.Context
import com.mindfulscreen.app.data.db.MindfulDatabase

/**
 * Lightweight manual dependency container. Created once in [com.mindfulscreen.app.MindfulScreenApp]
 * and reached from ViewModels via the Context extension, avoiding a DI framework for this MVP.
 */
class AppContainer(context: Context) {
    private val db = MindfulDatabase.get(context)

    val usageStatsRepository = UsageStatsRepository(context)
    val settingsRepository = SettingsRepository(context)
    val appLimitDao = db.appLimitDao()
    val dailyScoreDao = db.dailyScoreDao()
}
