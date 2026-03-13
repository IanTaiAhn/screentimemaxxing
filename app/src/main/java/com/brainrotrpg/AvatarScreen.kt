package com.brainrotrpg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    roomObjectViewModel: RoomObjectViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    val placedObjects by roomObjectViewModel.placedObjects.collectAsStateWithLifecycle()
    val characterState = remember { CharacterState() }

    var placementTarget: RoomObjectType? by remember { mutableStateOf(null) }
    var showShop by remember { mutableStateOf(false) }
    var inspectedObject: RoomObject? by remember { mutableStateOf(null) }

    AvatarScreenContent(
        uiState = uiState,
        placedObjects = placedObjects,
        characterState = characterState,
        placementTarget = placementTarget,
        onFloorTapped = { wx, wy ->
            val target = placementTarget
            if (target != null) {
                roomObjectViewModel.purchaseAndPlace(target, wx, wy)
                placementTarget = null
            } else {
                characterState.setTarget(wx, wy)
            }
        },
        onObjectTapped = { obj ->
            if (obj.isCurrentlyActive()) {
                inspectedObject = obj
            } else {
                roomObjectViewModel.activateObject(obj)
            }
        },
        onShopClick = { showShop = true },
        modifier = modifier
    )

    if (showShop) {
        ShopSheet(
            spendableBrainrotHours = uiState.spendableBrainrotHours,
            spendableMidHours = uiState.spendableMidHours,
            spendableEnrichmentHours = uiState.spendableEnrichmentHours,
            onDismiss = { showShop = false },
            onSelectObject = { type ->
                placementTarget = type
            }
        )
    }

    inspectedObject?.let { obj ->
        AlertDialog(
            onDismissRequest = { inspectedObject = null },
            title = { Text("${obj.objectType().emoji} ${obj.objectType().displayName}") },
            text = {
                val remaining = obj.timeRemainingMs()
                val hours = remaining / 3_600_000
                val minutes = (remaining % 3_600_000) / 60_000
                Text("${obj.objectType().description}\n\nActive for ${hours}h ${minutes}m remaining")
            },
            confirmButton = {
                TextButton(onClick = { inspectedObject = null }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    roomObjectViewModel.removeObject(obj)
                    inspectedObject = null
                }) { Text("Remove (50% refund)") }
            }
        )
    }
}

@Composable
private fun AvatarScreenContent(
    uiState: AvatarUiState,
    placedObjects: List<RoomObject> = emptyList(),
    characterState: CharacterState = remember { CharacterState() },
    placementTarget: RoomObjectType? = null,
    onFloorTapped: (Float, Float) -> Unit = { _, _ -> },
    onObjectTapped: (RoomObject) -> Unit = {},
    onShopClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val avatarClassName = when (uiState.avatarState) {
        is AvatarState.SigmaZombie -> stringResource(R.string.avatar_class_sigma_zombie)
        is AvatarState.ExtremelyOnline -> stringResource(R.string.avatar_class_extremely_online)
        is AvatarState.FakeIntellectual -> stringResource(R.string.avatar_class_fake_intellectual)
        is AvatarState.Hybrid -> stringResource(R.string.avatar_class_hybrid)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placement mode banner
            if (placementTarget != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tap the floor to place ${placementTarget.emoji} ${placementTarget.displayName}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Room scene — fills the top of the screen
            RoomScene(
                brainrotHours = uiState.brainrotHours,
                midHours = uiState.midHours,
                enrichmentHours = uiState.enrichmentHours,
                placedObjects = placedObjects,
                onFloorTapped = onFloorTapped,
                onObjectTapped = onObjectTapped,
                modifier = Modifier.fillMaxWidth(),
                characterState = characterState
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

            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }

        // Shop FAB
        FloatingActionButton(
            onClick = onShopClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("🛒")
        }
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
                enrichmentHours = 8f,
                spendableBrainrotHours = 1.5f,
                spendableMidHours = 0.5f,
                spendableEnrichmentHours = 2.0f
            )
        )
    }
}
