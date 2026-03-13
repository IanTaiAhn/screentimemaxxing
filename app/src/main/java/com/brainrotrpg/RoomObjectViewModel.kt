package com.brainrotrpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomObjectViewModel(
    private val roomObjectDao: RoomObjectDao,
    private val playerStatsDao: PlayerStatsDao
) : ViewModel() {

    val placedObjects: StateFlow<List<RoomObject>> = roomObjectDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Called when the player taps a placed object in the room. */
    fun activateObject(obj: RoomObject) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!obj.isCurrentlyActive(now)) {
                roomObjectDao.update(
                    obj.copy(
                        isActive = true,
                        activatedAt = now
                    )
                )
            }
            // If already active — tapping again could show info toast, or reset the timer.
            // For MVP: do nothing if already active (tap is a no-op).
        }
    }

    /** Purchase and place an object. Deducts hours from PlayerStats. */
    fun purchaseAndPlace(type: RoomObjectType, worldX: Float, worldY: Float) {
        viewModelScope.launch {
            val stats = playerStatsDao.getStats() ?: return@launch

            // Check the player can afford it
            val canAfford = when (type.costCategory) {
                Category.BRAINROT -> stats.spendableBrainrotHours >= type.costHours
                Category.MID -> stats.spendableMidHours >= type.costHours
                Category.ENRICHMENT -> stats.spendableEnrichmentHours >= type.costHours
                Category.UNTRACKED -> false
            }
            if (!canAfford) return@launch

            // Deduct hours
            val updatedStats = when (type.costCategory) {
                Category.BRAINROT -> stats.copy(
                    spendableBrainrotHours = stats.spendableBrainrotHours - type.costHours
                )
                Category.MID -> stats.copy(
                    spendableMidHours = stats.spendableMidHours - type.costHours
                )
                Category.ENRICHMENT -> stats.copy(
                    spendableEnrichmentHours = stats.spendableEnrichmentHours - type.costHours
                )
                Category.UNTRACKED -> stats
            }
            playerStatsDao.upsert(updatedStats)

            // Place the object
            roomObjectDao.insert(
                RoomObject(
                    type = type.name,
                    worldX = worldX,
                    worldY = worldY
                )
            )
        }
    }

    /** Remove a placed object and refund half its cost. */
    fun removeObject(obj: RoomObject) {
        viewModelScope.launch {
            val stats = playerStatsDao.getStats() ?: return@launch
            val type = obj.objectType()
            val refund = type.costHours * 0.5f

            val updatedStats = when (type.costCategory) {
                Category.BRAINROT -> stats.copy(
                    spendableBrainrotHours = stats.spendableBrainrotHours + refund
                )
                Category.MID -> stats.copy(
                    spendableMidHours = stats.spendableMidHours + refund
                )
                Category.ENRICHMENT -> stats.copy(
                    spendableEnrichmentHours = stats.spendableEnrichmentHours + refund
                )
                Category.UNTRACKED -> stats
            }
            playerStatsDao.upsert(updatedStats)
            roomObjectDao.delete(obj)
        }
    }

    companion object {
        fun factory(
            roomObjectDao: RoomObjectDao,
            playerStatsDao: PlayerStatsDao
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoomObjectViewModel(roomObjectDao, playerStatsDao)
            }
        }
    }
}
