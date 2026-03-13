package com.brainrotrpg

object LifecycleEngine {

    private const val LIFECYCLE_XP_THRESHOLD = 16_000L

    fun isLifecycleComplete(totalXp: Long): Boolean = totalXp >= LIFECYCLE_XP_THRESHOLD

    /**
     * Builds a LifecycleRecord snapshot from the current PlayerStats.
     * [roomObjectsPlaced] must be passed in from a separate DB query.
     */
    fun buildRecord(
        stats: PlayerStats,
        roomObjectsPlaced: Int,
        endedAt: Long = System.currentTimeMillis()
    ): LifecycleRecord {
        val outcome = resolveLifecycleOutcome(
            stats.brainrotHours,
            stats.midHours,
            stats.enrichmentHours
        )
        val avatarState = resolveAvatarState(
            stats.brainrotHours,
            stats.midHours,
            stats.enrichmentHours
        )
        val avatarClassName = when (avatarState) {
            is AvatarState.SigmaZombie -> "SigmaZombie"
            is AvatarState.ExtremelyOnline -> "ExtremelyOnline"
            is AvatarState.FakeIntellectual -> "FakeIntellectual"
            is AvatarState.Hybrid -> "Hybrid"
        }
        return LifecycleRecord(
            lifecycleNumber = stats.lifecycleNumber,
            outcome = outcome.name,
            finalAvatarClass = avatarClassName,
            totalXp = stats.totalXp,
            finalLevel = stats.level,
            brainrotHours = stats.brainrotHours,
            midHours = stats.midHours,
            enrichmentHours = stats.enrichmentHours,
            totalHours = stats.brainrotHours + stats.midHours + stats.enrichmentHours,
            roomObjectsPlaced = roomObjectsPlaced,
            startedAt = stats.lifecycleStartedAt,
            endedAt = endedAt
        )
    }

    /**
     * Produces a fresh PlayerStats row for the next life.
     * Carries over only the lifecycle number (incremented) and a new start timestamp.
     */
    fun resetStats(currentStats: PlayerStats, now: Long = System.currentTimeMillis()): PlayerStats {
        return PlayerStats(
            id = 1,
            totalXp = 0L,
            level = 1,
            brainrotHours = 0f,
            midHours = 0f,
            enrichmentHours = 0f,
            spendableBrainrotHours = 0f,
            spendableMidHours = 0f,
            spendableEnrichmentHours = 0f,
            lastCheckedTimestamp = now,
            lifecycleNumber = currentStats.lifecycleNumber + 1,
            lifecycleStartedAt = now,
            pendingLifecycleEnd = false
        )
    }
}
