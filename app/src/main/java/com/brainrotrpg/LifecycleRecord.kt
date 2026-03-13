package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lifecycle_records")
data class LifecycleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lifecycleNumber: Int,           // 1-indexed, increments with each new life
    val outcome: String,                // LifecycleOutcome.name
    val finalAvatarClass: String,       // AvatarState class name at time of end
    val totalXp: Long,
    val finalLevel: Int,
    val brainrotHours: Float,
    val midHours: Float,
    val enrichmentHours: Float,
    val totalHours: Float,              // brainrot + mid + enrichment
    val roomObjectsPlaced: Int,         // total objects placed during this life
    val startedAt: Long,                // timestamp when this life began
    val endedAt: Long                   // timestamp when this life ended
)

fun LifecycleRecord.outcome(): LifecycleOutcome = LifecycleOutcome.valueOf(outcome)
fun LifecycleRecord.avatarState(): AvatarState = when (finalAvatarClass) {
    "SigmaZombie" -> AvatarState.SigmaZombie
    "ExtremelyOnline" -> AvatarState.ExtremelyOnline
    "FakeIntellectual" -> AvatarState.FakeIntellectual
    else -> AvatarState.Hybrid
}

// Derived stat: dominant category as a readable label
fun LifecycleRecord.dominantCategory(): String {
    val total = totalHours
    if (total == 0f) return "None"
    return when {
        brainrotHours / total > 0.6f -> "Brainrot"
        midHours / total > 0.6f -> "Mid"
        enrichmentHours / total > 0.6f -> "Enrichment"
        else -> "Hybrid"
    }
}
