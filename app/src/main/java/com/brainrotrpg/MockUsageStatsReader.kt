package com.brainrotrpg

import android.content.Context

object MockUsageStatsReader : IUsageStatsReader {

    // Hardcoded fake usage data for development and testing:
    // 3 hours TikTok (BRAINROT), 1 hour Spotify (ENRICHMENT), 30 min YouTube (MID)
    private val mockPackageUsage: Map<String, Long> = mapOf(
        "com.zhiliaoapp.musically" to 3L * 60 * 60 * 1000,  // 3 hours TikTok
        "com.spotify.music" to 1L * 60 * 60 * 1000,          // 1 hour Spotify
        "com.google.android.youtube" to 30L * 60 * 1000       // 30 min YouTube
    )

    override fun getUsageSince(context: Context, sinceMillis: Long): Map<String, Long> {
        return mockPackageUsage
    }

    override fun getCategorizedUsage(context: Context, sinceMillis: Long): Map<Category, Long> {
        return UsageStatsReader.aggregateCategorizedUsage(mockPackageUsage)
    }
}
