package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LifecycleEndScreen(
    record: LifecycleRecord,
    onBeginNewLife: () -> Unit,
    onViewArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = record.outcome()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = outcome.emoji,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Life ${record.lifecycleNumber} — ${outcome.displayName}",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = outcome.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // Stats summary card
        LifecycleStatsSummary(record = record)

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onBeginNewLife,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Begin New Life")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onViewArchive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Archive")
        }
    }
}

@Composable
private fun LifecycleStatsSummary(
    record: LifecycleRecord,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Final Stats", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StatLine("Avatar Class", record.avatarState().displayName())
            StatLine("Total Hours", "%.1f hrs".format(record.totalHours))
            StatLine("🧟 Brainrot", "%.1f hrs".format(record.brainrotHours))
            StatLine("😐 Mid", "%.1f hrs".format(record.midHours))
            StatLine("🎧 Enrichment", "%.1f hrs".format(record.enrichmentHours))
            StatLine("Objects Placed", "${record.roomObjectsPlaced}")
            StatLine("Final Level", "${record.finalLevel}")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// Helper to get a display name from AvatarState
private fun AvatarState.displayName(): String = when (this) {
    is AvatarState.SigmaZombie -> "Sigma Zombie"
    is AvatarState.ExtremelyOnline -> "Extremely Online"
    is AvatarState.FakeIntellectual -> "Fake Intellectual"
    is AvatarState.Hybrid -> "Hybrid"
}
