package com.mindfulscreen.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindfulscreen.app.ui.util.formatMinutes

/**
 * Three-step onboarding:
 *  1. Welcome + why
 *  2. Grant Usage Access (deep-links to system settings)
 *  3. Set a daily goal
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Re-check permission whenever we resume from the settings screen.
    LaunchedEffect(Unit) { viewModel.refreshPermission() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("\uD83E\uDDE0", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Welcome to MindfulScreen",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "See your real screen time, set gentle limits, and keep your attention " +
                "score healthy. No accounts, no cloud — everything stays on your phone.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // Step 2: permission
        Card(Modifier.padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("1. Grant Usage Access",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "MindfulScreen needs Usage Access to read how long you spend in each " +
                        "app. It never reads your messages, photos, or content.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (state.hasPermission) {
                    Text("\u2705 Access granted",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                } else {
                    OutlinedButton(onClick = {
                        context.startActivity(viewModel.settingsIntent())
                    }) { Text("Open Usage Access settings") }
                }
            }
        }

        // Step 3: goal
        Card(Modifier.padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("2. Set your daily goal",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Daily screen-time goal: ${formatMinutes(state.goalMinutes.toLong())}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Slider(
                    value = state.goalMinutes.toFloat(),
                    onValueChange = { viewModel.setGoalMinutes(it.toInt()) },
                    valueRange = 30f..360f,
                    steps = 10,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.finish(onFinished) },
            enabled = state.hasPermission,
        ) { Text(if (state.hasPermission) "Get Started" else "Grant access to continue") }
    }
}
