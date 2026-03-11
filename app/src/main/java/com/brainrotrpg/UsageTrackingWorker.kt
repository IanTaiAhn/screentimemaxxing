package com.brainrotrpg

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UsageTrackingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UsageTrackingWorker"
        private const val MS_PER_HOUR = 3_600_000f
        private const val DEFAULT_LOOKBACK_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val playerStatsDao = db.playerStatsDao()
            val usageRecordDao = db.usageRecordDao()

            // Step 1: Read lastCheckedTimestamp from PlayerStats
            val currentStats = playerStatsDao.getStats()
            val now = System.currentTimeMillis()
            val sinceMillis = currentStats?.lastCheckedTimestamp ?: (now - DEFAULT_LOOKBACK_MS)

            // Step 2: Get categorized usage since last check
            val reader = UsageStatsReaderProvider.instance
            val categorizedUsage = reader.getCategorizedUsage(applicationContext, sinceMillis)

            // Step 3: Insert a UsageRecord for each category with duration
            for ((category, durationMillis) in categorizedUsage) {
                if (durationMillis > 0) {
                    usageRecordDao.insert(
                        UsageRecord(
                            timestamp = now,
                            category = category.name,
                            durationMillis = durationMillis
                        )
                    )
                }
            }

            // Step 4: Update PlayerStats — add hours, recalculate XP, update timestamp
            val brainrotDeltaHours = (categorizedUsage[Category.BRAINROT] ?: 0L) / MS_PER_HOUR
            val midDeltaHours = (categorizedUsage[Category.MID] ?: 0L) / MS_PER_HOUR
            val enrichmentDeltaHours = (categorizedUsage[Category.ENRICHMENT] ?: 0L) / MS_PER_HOUR

            val newBrainrotHours = (currentStats?.brainrotHours ?: 0f) + brainrotDeltaHours
            val newMidHours = (currentStats?.midHours ?: 0f) + midDeltaHours
            val newEnrichmentHours = (currentStats?.enrichmentHours ?: 0f) + enrichmentDeltaHours

            // XP: 1 hour = 100 XP for all categories (will be encapsulated by XpEngine in Task 4.2)
            val totalHours = newBrainrotHours + newMidHours + newEnrichmentHours
            val newTotalXp = (totalHours * 100).toLong()
            val newLevel = calculateLevel(newTotalXp)

            val updatedStats = PlayerStats(
                id = 1,
                totalXp = newTotalXp,
                level = newLevel,
                brainrotHours = newBrainrotHours,
                midHours = newMidHours,
                enrichmentHours = newEnrichmentHours,
                lastCheckedTimestamp = now
            )
            playerStatsDao.upsert(updatedStats)

            Log.d(TAG, "Usage tracking complete. XP: $newTotalXp, Level: $newLevel")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Usage tracking failed, will retry", e)
            Result.retry()
        }
    }

    // Placeholder level thresholds — will be replaced by XpEngine in Task 4.2
    private fun calculateLevel(totalXp: Long): Int {
        return when {
            totalXp >= 3000 -> 5
            totalXp >= 2000 -> 4
            totalXp >= 1200 -> 3
            totalXp >= 500 -> 2
            else -> 1
        }
    }
}
