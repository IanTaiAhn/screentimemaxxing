package com.brainrotrpg

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

object UsageStatsReader {

    private const val TAG = "UsageStatsReader"

    fun getUsageSince(context: Context, sinceMillis: Long): Map<String, Long> {
        if (!UsagePermissionHelper.hasUsagePermission(context)) {
            Log.w(TAG, "Usage stats permission not granted. Returning empty map.")
            return emptyMap()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            sinceMillis,
            now
        ) ?: return emptyMap()

        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .groupingBy { it.packageName }
            .fold(0L) { acc, stats -> acc + stats.totalTimeInForeground }
    }

    fun getCategorizedUsage(context: Context, sinceMillis: Long): Map<Category, Long> {
        val usageMap = getUsageSince(context, sinceMillis)
        return aggregateCategorizedUsage(usageMap)
    }

    internal fun aggregateCategorizedUsage(usageMap: Map<String, Long>): Map<Category, Long> {
        val result = mutableMapOf<Category, Long>()
        for ((packageName, durationMillis) in usageMap) {
            val category = getCategory(packageName)
            result[category] = (result[category] ?: 0L) + durationMillis
        }
        return result
    }
}
