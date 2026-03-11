package com.brainrotrpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageStatsReaderTest {

    @Test
    fun `aggregateCategorizedUsage returns empty map for empty input`() {
        val result = UsageStatsReader.aggregateCategorizedUsage(emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `aggregateCategorizedUsage correctly categorizes brainrot apps`() {
        val usageMap = mapOf(
            "com.zhiliaoapp.musically" to 60_000L,  // TikTok - 1 min
            "com.instagram.android" to 120_000L     // Instagram - 2 min
        )
        val result = UsageStatsReader.aggregateCategorizedUsage(usageMap)
        assertEquals(180_000L, result[Category.BRAINROT])
    }

    @Test
    fun `aggregateCategorizedUsage correctly categorizes mid apps`() {
        val usageMap = mapOf(
            "com.google.android.youtube" to 300_000L,  // YouTube - 5 min
            "com.twitter.android" to 60_000L,          // Twitter - 1 min
            "com.reddit.frontpage" to 120_000L         // Reddit - 2 min
        )
        val result = UsageStatsReader.aggregateCategorizedUsage(usageMap)
        assertEquals(480_000L, result[Category.MID])
    }

    @Test
    fun `aggregateCategorizedUsage correctly categorizes enrichment apps`() {
        val usageMap = mapOf(
            "com.spotify.music" to 3_600_000L,                 // Spotify - 1 hour
            "com.audible.application" to 1_800_000L,           // Audible - 30 min
            "com.google.android.apps.podcasts" to 900_000L     // Podcasts - 15 min
        )
        val result = UsageStatsReader.aggregateCategorizedUsage(usageMap)
        assertEquals(6_300_000L, result[Category.ENRICHMENT])
    }

    @Test
    fun `aggregateCategorizedUsage places unknown packages in UNTRACKED`() {
        val usageMap = mapOf(
            "com.unknown.app" to 60_000L,
            "com.another.unknown" to 30_000L
        )
        val result = UsageStatsReader.aggregateCategorizedUsage(usageMap)
        assertEquals(90_000L, result[Category.UNTRACKED])
    }

    @Test
    fun `aggregateCategorizedUsage correctly aggregates mixed categories`() {
        val usageMap = mapOf(
            "com.zhiliaoapp.musically" to 600_000L,    // TikTok - BRAINROT
            "com.spotify.music" to 3_600_000L,         // Spotify - ENRICHMENT
            "com.google.android.youtube" to 1_800_000L, // YouTube - MID
            "com.unknown.app" to 60_000L               // Unknown - UNTRACKED
        )
        val result = UsageStatsReader.aggregateCategorizedUsage(usageMap)
        assertEquals(600_000L, result[Category.BRAINROT])
        assertEquals(3_600_000L, result[Category.ENRICHMENT])
        assertEquals(1_800_000L, result[Category.MID])
        assertEquals(60_000L, result[Category.UNTRACKED])
    }
}
