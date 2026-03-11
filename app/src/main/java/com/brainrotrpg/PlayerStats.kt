package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey
    val id: Int = 1, // Singleton row — always 1
    val totalXp: Long,
    val level: Int,
    val brainrotHours: Float,
    val midHours: Float,
    val enrichmentHours: Float,
    val lastCheckedTimestamp: Long
)
