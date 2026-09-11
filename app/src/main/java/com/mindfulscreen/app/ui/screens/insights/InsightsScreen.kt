package com.mindfulscreen.app.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindfulscreen.app.ui.theme.Blue500
import com.mindfulscreen.app.ui.theme.Green
import com.mindfulscreen.app.ui.theme.Red
import com.mindfulscreen.app.ui.util.formatDuration
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory),
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // 7-day bar chart
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val maxMs = (state.week.maxOfOrNull { it.totalMs } ?: 1L).coerceAtLeast(1L)
                Row(
                    Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    state.week.forEach { bar ->
                        val frac = bar.totalMs.toFloat() / maxMs
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height((110 * frac).dp.coerceAtLeast(2.dp))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Blue500)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                bar.date.dayOfWeek.getDisplayName(
                                    TextStyle.SHORT, Locale.getDefault()
                                ).take(2),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // vs last week
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("vs. last week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val pct = state.weekOverWeekPct
                if (pct == null) {
                    Text("Not enough history yet — check back in a week.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    val improved = pct <= 0
                    Text(
                        (if (improved) "▼ " else "▲ ") + "${kotlin.math.abs(pct)}% " +
                            (if (improved) "less screen time" else "more screen time"),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (improved) Green else Red,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "This week ${formatDuration(state.thisWeekMs)} · " +
                            "last week ${formatDuration(state.lastWeekMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Highlights
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This week's highlights", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                state.mostUsedLabel?.let {
                    Text("Most used: $it (${formatDuration(state.mostUsedMs)})",
                        style = MaterialTheme.typography.bodyLarge)
                }
                Text("Late-night use (00:00–06:00): ${formatDuration(state.lateNightMs)}",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
