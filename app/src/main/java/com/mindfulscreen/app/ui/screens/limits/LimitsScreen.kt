package com.mindfulscreen.app.ui.screens.limits

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.mindfulscreen.app.ui.util.formatDuration
import com.mindfulscreen.app.ui.util.formatMinutes

@Composable
fun LimitsScreen(
    viewModel: LimitsViewModel = viewModel(factory = LimitsViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Goals & Limits", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Daily goal
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Daily screen-time goal", style = MaterialTheme.typography.titleMedium)
                Text(formatMinutes(state.goalMinutes.toLong()),
                    style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = state.goalMinutes.toFloat(),
                    onValueChange = { viewModel.setGoalMinutes(it.toInt()) },
                    valueRange = 30f..360f,
                    steps = 10,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Active per-app limits", style = MaterialTheme.typography.titleMedium)
        if (state.limits.isEmpty()) {
            Text("No limits yet. Add one from your top apps below.",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            state.limits.forEach { limit ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(limit.label, fontWeight = FontWeight.Medium)
                            Text("Limit: ${formatDuration(limit.limitMs)}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.removeLimit(limit) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove limit")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Set a limit from your top apps", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        val limitedPkgs = state.limits.map { it.packageName }.toSet()
        state.candidateApps
            .filter { it.packageName !in limitedPkgs }
            .forEach { app ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text("today: ${formatDuration(app.timeMs)}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Tap to set a 30-minute limit",
                            style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { viewModel.setLimit(app, 30) },
                        ) { Text("Limit to 30m") }
                    }
                }
            }
        Spacer(Modifier.height(40.dp))
    }
}
