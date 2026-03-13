package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey
    val id: Int = 1, // Singleton row — always 1
    val totalXp: Long,
    val level: Int,
    val brainrotHours: Float,       // cumulative — used for avatar class
    val midHours: Float,
    val enrichmentHours: Float,
    val spendableBrainrotHours: Float = 0f,   // spendable currency
    val spendableMidHours: Float = 0f,
    val spendableEnrichmentHours: Float = 0f,
    val lastCheckedTimestamp: Long,
    val lifecycleNumber: Int = 1,           // which life this is
    val lifecycleStartedAt: Long = 0L,      // timestamp when this life began
    val pendingLifecycleEnd: Boolean = false // true when lifecycle has just ended, awaiting player acknowledgment
)
