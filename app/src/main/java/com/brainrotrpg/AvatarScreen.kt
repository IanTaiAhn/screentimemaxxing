package com.brainrotrpg

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    val avatarDrawableRes = when (uiState.avatarState) {
        is AvatarState.SigmaZombie -> R.drawable.avatar_sigma_zombie
        is AvatarState.ExtremelyOnline -> R.drawable.avatar_extremely_online
        is AvatarState.FakeIntellectual -> R.drawable.avatar_fake_intellectual
        is AvatarState.Hybrid -> R.drawable.avatar_hybrid
    }

    val avatarClassName = when (uiState.avatarState) {
        is AvatarState.SigmaZombie -> stringResource(R.string.avatar_class_sigma_zombie)
        is AvatarState.ExtremelyOnline -> stringResource(R.string.avatar_class_extremely_online)
        is AvatarState.FakeIntellectual -> stringResource(R.string.avatar_class_fake_intellectual)
        is AvatarState.Hybrid -> stringResource(R.string.avatar_class_hybrid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = avatarDrawableRes),
            contentDescription = avatarClassName,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = stringResource(R.string.level_badge, uiState.level),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = avatarClassName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
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
                brainrotHours = 10f,
                midHours = 2f,
                enrichmentHours = 3f
            )
        )
    }
}
