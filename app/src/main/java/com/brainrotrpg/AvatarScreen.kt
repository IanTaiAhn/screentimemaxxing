package com.brainrotrpg

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brainrotrpg.ui.theme.BrainRotRPGTheme

@Composable
fun AvatarScreen(
    avatarViewModel: AvatarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    AvatarScreenContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun AvatarScreenContent(
    uiState: AvatarUiState,
    modifier: Modifier = Modifier
) {
    val avatarClassName = when (uiState.avatarState) {
        is AvatarState.SigmaZombie -> stringResource(R.string.avatar_class_sigma_zombie)
        is AvatarState.ExtremelyOnline -> stringResource(R.string.avatar_class_extremely_online)
        is AvatarState.FakeIntellectual -> stringResource(R.string.avatar_class_fake_intellectual)
        is AvatarState.Hybrid -> stringResource(R.string.avatar_class_hybrid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Room scene — fills the top of the screen
        RoomScene(
            brainrotHours = uiState.brainrotHours,
            midHours = uiState.midHours,
            enrichmentHours = uiState.enrichmentHours,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Level badge + class name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(R.string.level_badge, uiState.level),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Text(
                text = avatarClassName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // XP progress bar
        LinearProgressIndicator(
            progress = { uiState.xpProgressFraction },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.xp_progress,
                uiState.totalXp,
                uiState.totalXp + uiState.xpToNextLevel
            ),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats breakdown
        StatsBreakdownSection(
            brainrotHours = uiState.brainrotHours,
            midHours = uiState.midHours,
            enrichmentHours = uiState.enrichmentHours,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatsBreakdownSection(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float,
    modifier: Modifier = Modifier
) {
    val totalHours = brainrotHours + midHours + enrichmentHours

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.stats_section_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        StatRow(
            label = stringResource(R.string.stat_brainrot),
            hours = brainrotHours,
            fraction = if (totalHours > 0f) brainrotHours / totalHours else 0f
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatRow(
            label = stringResource(R.string.stat_mid),
            hours = midHours,
            fraction = if (totalHours > 0f) midHours / totalHours else 0f
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatRow(
            label = stringResource(R.string.stat_enrichment),
            hours = enrichmentHours,
            fraction = if (totalHours > 0f) enrichmentHours / totalHours else 0f
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    hours: Float,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.stat_hours, hours),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarScreenPreview() {
    BrainRotRPGTheme {
        AvatarScreenContent(
            uiState = AvatarUiState(
                level = 3,
                totalXp = 1500L,
                xpToNextLevel = 500L,
                xpProgressFraction = 0.375f,
                avatarState = AvatarState.SigmaZombie,
                brainrotHours = 6f,
                midHours = 2f,
                enrichmentHours = 8f
            )
        )
    }
}
