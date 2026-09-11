package com.mindfulscreen.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/** App navigation routes. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val INSIGHTS = "insights"
    const val LIMITS = "limits"
}

/** Bottom-navigation tabs shown after onboarding. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Filled.Dashboard),
    INSIGHTS(Routes.INSIGHTS, "Insights", Icons.Filled.Insights),
    LIMITS(Routes.LIMITS, "Limits", Icons.Filled.Tune),
}
