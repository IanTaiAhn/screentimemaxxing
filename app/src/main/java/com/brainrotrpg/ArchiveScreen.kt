package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArchiveScreen(
    viewModel: LifecycleViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Archive",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No lives completed yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    ArchiveCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun ArchiveCard(
    record: LifecycleRecord,
    modifier: Modifier = Modifier
) {
    val outcome = record.outcome()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${outcome.emoji} Life ${record.lifecycleNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = outcome.displayName,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = record.dominantCategory() + " · " +
                       "%.0f hrs total".format(record.totalHours) + " · " +
                       "Level ${record.finalLevel}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryPill("🧟 %.0fh".format(record.brainrotHours))
                CategoryPill("😐 %.0fh".format(record.midHours))
                CategoryPill("🎧 %.0fh".format(record.enrichmentHours))
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
