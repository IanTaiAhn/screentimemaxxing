package com.brainrotrpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LifecycleViewModel(
    private val lifecycleRecordDao: LifecycleRecordDao,
    private val playerStatsDao: PlayerStatsDao
) : ViewModel() {

    val allRecords: StateFlow<List<LifecycleRecord>> = lifecycleRecordDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mostRecentRecord: StateFlow<LifecycleRecord?> = lifecycleRecordDao.observeAll()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Called when the player taps "Begin New Life". Resets PlayerStats and clears all room objects. */
    fun beginNewLife() {
        viewModelScope.launch {
            val current = playerStatsDao.getStats() ?: return@launch
            val resetStats = LifecycleEngine.resetStats(current)
            playerStatsDao.upsert(resetStats)
        }
    }

    companion object {
        fun factory(
            lifecycleRecordDao: LifecycleRecordDao,
            playerStatsDao: PlayerStatsDao
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LifecycleViewModel(lifecycleRecordDao, playerStatsDao)
            }
        }
    }
}
