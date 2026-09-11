package com.mindfulscreen.app.intervention

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindfulscreen.app.ui.theme.MindfulScreenTheme

/**
 * Full-screen "mindful nudge" shown when a per-app or daily limit is exceeded.
 * Deliberately adds a small moment of friction (nudge theory) rather than a hard block.
 */
class NudgeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "an app"
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        setContent {
            MindfulScreenTheme {
                Surface(Modifier.fillMaxSize()) {
                    NudgeContent(
                        appLabel = appLabel,
                        minutes = minutes,
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_MINUTES = "minutes"
    }
}

@Composable
private fun NudgeContent(appLabel: String, minutes: Int, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("\uD83E\uDEE0", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "You've used $appLabel for $minutes minutes today.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "That's past your limit. Take a breath — is this the best use of your attention right now?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss) { Text("Okay, I'll put it down") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss) { Text("Keep going for now") }
    }
}
