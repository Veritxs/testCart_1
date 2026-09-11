package com.mindfulscreen.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindfulscreen.app.ui.components.ScoreRing
import com.mindfulscreen.app.ui.util.formatDuration

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    if (state.loading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        return
    }

    if (!state.hasPermission) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Usage Access is off", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant Usage Access in Settings so MindfulScreen can read your screen time.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val progress = if (state.goalMs > 0)
        (state.totalMs.toFloat() / state.goalMs).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar + mood caption
        Text(state.mood.emoji, style = MaterialTheme.typography.headlineLarge)
        Text(state.mood.caption, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        // Attention score ring
        ScoreRing(score = state.score)
        Text("Attention Score", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))

        // Today vs goal
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatDuration(state.totalMs)} / ${formatDuration(state.goalMs)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                if (state.brokenLimits.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Over limit: ${state.brokenLimits.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Top apps today
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Top apps today", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (state.topApps.isEmpty()) {
                    Text("No usage recorded yet today.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    val max = state.topApps.first().timeMs.coerceAtLeast(1)
                    state.topApps.forEach { app ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(formatDuration(app.timeMs))
                        }
                        LinearProgressIndicator(
                            progress = { (app.timeMs.toFloat() / max) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                        )
                    }
                }
            }
        }
    }
}
