package com.brainrotrpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AvatarUiState(
    val level: Int = 1,
    val totalXp: Long = 0L,
    val xpToNextLevel: Long = 500L,
    val xpProgressFraction: Float = 0f,
    val avatarState: AvatarState = AvatarState.Hybrid,
    val brainrotHours: Float = 0f,
    val midHours: Float = 0f,
    val enrichmentHours: Float = 0f,
    val spendableBrainrotHours: Float = 0f,
    val spendableMidHours: Float = 0f,
    val spendableEnrichmentHours: Float = 0f
)

class AvatarViewModel(
    private val playerStatsDao: PlayerStatsDao
) : ViewModel() {

    val uiState: StateFlow<AvatarUiState> = playerStatsDao.observeStats()
        .map { stats -> stats?.toUiState() ?: AvatarUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AvatarUiState()
        )

    val pendingLifecycleEnd: StateFlow<Boolean> = playerStatsDao.observeStats()
        .filterNotNull()
        .map { stats -> stats.pendingLifecycleEnd }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private fun PlayerStats.toUiState(): AvatarUiState {
        val xpRemaining = XpEngine.xpToNextLevel(totalXp)
        val levelStart = XpEngine.currentLevelThreshold(totalXp)
        val xpEarnedInLevel = totalXp - levelStart
        val totalXpForLevel = xpEarnedInLevel + xpRemaining
        val progressFraction = if (totalXpForLevel == 0L) 1f
            else xpEarnedInLevel.toFloat() / totalXpForLevel.toFloat()
        return AvatarUiState(
            level = level,
            totalXp = totalXp,
            xpToNextLevel = xpRemaining,
            xpProgressFraction = progressFraction,
            avatarState = resolveAvatarState(brainrotHours, midHours, enrichmentHours),
            brainrotHours = brainrotHours,
            midHours = midHours,
            enrichmentHours = enrichmentHours,
            spendableBrainrotHours = spendableBrainrotHours,
            spendableMidHours = spendableMidHours,
            spendableEnrichmentHours = spendableEnrichmentHours
        )
    }

    companion object {
        fun factory(playerStatsDao: PlayerStatsDao): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AvatarViewModel(playerStatsDao)
                }
            }
    }
}
