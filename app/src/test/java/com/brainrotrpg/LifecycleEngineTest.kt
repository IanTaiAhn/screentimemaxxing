package com.brainrotrpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleEngineTest {

    // ----------------------------
    // resolveLifecycleOutcome tests
    // ----------------------------

    @Test
    fun `resolveLifecycleOutcome returns ASCEND for zero hours`() {
        assertEquals(LifecycleOutcome.ASCEND, resolveLifecycleOutcome(0f, 0f, 0f))
    }

    @Test
    fun `resolveLifecycleOutcome returns GRADUATE when enrichment is dominant`() {
        // 70% enrichment — above 60% threshold
        assertEquals(LifecycleOutcome.GRADUATE, resolveLifecycleOutcome(0f, 0f, 7f))
        assertEquals(LifecycleOutcome.GRADUATE, resolveLifecycleOutcome(1f, 2f, 7f))
    }

    @Test
    fun `resolveLifecycleOutcome returns DIE when brainrot is dominant`() {
        // 70% brainrot — above 60% threshold
        assertEquals(LifecycleOutcome.DIE, resolveLifecycleOutcome(7f, 0f, 0f))
        assertEquals(LifecycleOutcome.DIE, resolveLifecycleOutcome(7f, 2f, 1f))
    }

    @Test
    fun `resolveLifecycleOutcome returns ASCEND for hybrid usage`() {
        // No category above 60%
        assertEquals(LifecycleOutcome.ASCEND, resolveLifecycleOutcome(4f, 3f, 3f))
        assertEquals(LifecycleOutcome.ASCEND, resolveLifecycleOutcome(5f, 3f, 2f))
    }

    @Test
    fun `resolveLifecycleOutcome returns ASCEND when mid is dominant`() {
        // 70% mid — no ASCEND path for mid, so falls to else branch
        assertEquals(LifecycleOutcome.ASCEND, resolveLifecycleOutcome(0f, 7f, 0f))
    }

    @Test
    fun `resolveLifecycleOutcome treats exactly 60 percent enrichment as not dominant`() {
        // exactly 60% enrichment — NOT > 0.6f, so not GRADUATE
        val result = resolveLifecycleOutcome(2f, 0f, 3f) // 60% enrichment
        assertEquals(LifecycleOutcome.ASCEND, result)
    }

    @Test
    fun `resolveLifecycleOutcome treats exactly 60 percent brainrot as not dominant`() {
        // exactly 60% brainrot — NOT > 0.6f, so not DIE
        val result = resolveLifecycleOutcome(3f, 0f, 2f) // 60% brainrot
        assertEquals(LifecycleOutcome.ASCEND, result)
    }

    @Test
    fun `resolveLifecycleOutcome enrichment takes precedence when both enrichment and brainrot high`() {
        // enrichment checked first — 61% enrichment, 39% brainrot
        val result = resolveLifecycleOutcome(3.9f, 0f, 6.1f)
        assertEquals(LifecycleOutcome.GRADUATE, result)
    }

    // ----------------------------
    // LifecycleEngine.isLifecycleComplete tests
    // ----------------------------

    @Test
    fun `isLifecycleComplete returns false for XP below threshold`() {
        assertFalse(LifecycleEngine.isLifecycleComplete(15_999L))
        assertFalse(LifecycleEngine.isLifecycleComplete(0L))
    }

    @Test
    fun `isLifecycleComplete returns true at exactly 16000 XP`() {
        assertTrue(LifecycleEngine.isLifecycleComplete(16_000L))
    }

    @Test
    fun `isLifecycleComplete returns true for XP above threshold`() {
        assertTrue(LifecycleEngine.isLifecycleComplete(16_001L))
        assertTrue(LifecycleEngine.isLifecycleComplete(99_999L))
    }

    // ----------------------------
    // LifecycleEngine.buildRecord tests
    // ----------------------------

    private fun makeStats(
        brainrot: Float = 0f,
        mid: Float = 0f,
        enrichment: Float = 0f,
        totalXp: Long = 16_000L,
        level: Int = 10,
        lifecycleNumber: Int = 1,
        lifecycleStartedAt: Long = 1000L
    ) = PlayerStats(
        id = 1,
        totalXp = totalXp,
        level = level,
        brainrotHours = brainrot,
        midHours = mid,
        enrichmentHours = enrichment,
        lastCheckedTimestamp = System.currentTimeMillis(),
        lifecycleNumber = lifecycleNumber,
        lifecycleStartedAt = lifecycleStartedAt
    )

    @Test
    fun `buildRecord populates all fields from PlayerStats`() {
        val stats = makeStats(brainrot = 80f, mid = 10f, enrichment = 10f, totalXp = 16_000L, level = 10, lifecycleNumber = 2, lifecycleStartedAt = 5000L)
        val endedAt = 9999L
        val record = LifecycleEngine.buildRecord(stats, roomObjectsPlaced = 7, endedAt = endedAt)

        assertEquals(2, record.lifecycleNumber)
        assertEquals(LifecycleOutcome.DIE.name, record.outcome)
        assertEquals("SigmaZombie", record.finalAvatarClass)
        assertEquals(16_000L, record.totalXp)
        assertEquals(10, record.finalLevel)
        assertEquals(80f, record.brainrotHours, 0.001f)
        assertEquals(10f, record.midHours, 0.001f)
        assertEquals(10f, record.enrichmentHours, 0.001f)
        assertEquals(100f, record.totalHours, 0.001f)
        assertEquals(7, record.roomObjectsPlaced)
        assertEquals(5000L, record.startedAt)
        assertEquals(endedAt, record.endedAt)
    }

    @Test
    fun `buildRecord produces GRADUATE outcome for enrichment-heavy stats`() {
        val stats = makeStats(brainrot = 10f, mid = 10f, enrichment = 80f)
        val record = LifecycleEngine.buildRecord(stats, roomObjectsPlaced = 0)
        assertEquals(LifecycleOutcome.GRADUATE.name, record.outcome)
        assertEquals("FakeIntellectual", record.finalAvatarClass)
    }

    @Test
    fun `buildRecord produces ASCEND outcome for hybrid stats`() {
        val stats = makeStats(brainrot = 30f, mid = 35f, enrichment = 35f)
        val record = LifecycleEngine.buildRecord(stats, roomObjectsPlaced = 3)
        assertEquals(LifecycleOutcome.ASCEND.name, record.outcome)
        assertEquals("Hybrid", record.finalAvatarClass)
    }

    @Test
    fun `buildRecord totalHours is sum of all category hours`() {
        val stats = makeStats(brainrot = 50f, mid = 30f, enrichment = 20f)
        val record = LifecycleEngine.buildRecord(stats, roomObjectsPlaced = 0)
        assertEquals(100f, record.totalHours, 0.001f)
    }

    // ----------------------------
    // LifecycleEngine.resetStats tests
    // ----------------------------

    @Test
    fun `resetStats zeroes all XP and hours`() {
        val current = makeStats(brainrot = 80f, mid = 10f, enrichment = 10f, totalXp = 16_000L, level = 10, lifecycleNumber = 1)
        val reset = LifecycleEngine.resetStats(current, now = 12345L)

        assertEquals(0L, reset.totalXp)
        assertEquals(1, reset.level)
        assertEquals(0f, reset.brainrotHours, 0.001f)
        assertEquals(0f, reset.midHours, 0.001f)
        assertEquals(0f, reset.enrichmentHours, 0.001f)
        assertEquals(0f, reset.spendableBrainrotHours, 0.001f)
        assertEquals(0f, reset.spendableMidHours, 0.001f)
        assertEquals(0f, reset.spendableEnrichmentHours, 0.001f)
    }

    @Test
    fun `resetStats increments lifecycle number`() {
        val current = makeStats(lifecycleNumber = 3)
        val reset = LifecycleEngine.resetStats(current)
        assertEquals(4, reset.lifecycleNumber)
    }

    @Test
    fun `resetStats sets pendingLifecycleEnd to false`() {
        val current = PlayerStats(
            id = 1,
            totalXp = 16_000L,
            level = 10,
            brainrotHours = 80f,
            midHours = 10f,
            enrichmentHours = 10f,
            lastCheckedTimestamp = 0L,
            lifecycleNumber = 1,
            lifecycleStartedAt = 0L,
            pendingLifecycleEnd = true
        )
        val reset = LifecycleEngine.resetStats(current)
        assertFalse(reset.pendingLifecycleEnd)
    }

    @Test
    fun `resetStats updates lifecycleStartedAt to now`() {
        val current = makeStats(lifecycleStartedAt = 1000L)
        val now = 99_999L
        val reset = LifecycleEngine.resetStats(current, now = now)
        assertEquals(now, reset.lifecycleStartedAt)
        assertEquals(now, reset.lastCheckedTimestamp)
    }

    @Test
    fun `resetStats preserves singleton id`() {
        val current = makeStats()
        val reset = LifecycleEngine.resetStats(current)
        assertEquals(1, reset.id)
    }

    // ----------------------------
    // LifecycleRecord.dominantCategory tests
    // ----------------------------

    private fun makeRecord(
        brainrot: Float,
        mid: Float,
        enrichment: Float
    ) = LifecycleRecord(
        lifecycleNumber = 1,
        outcome = LifecycleOutcome.ASCEND.name,
        finalAvatarClass = "Hybrid",
        totalXp = 1000L,
        finalLevel = 5,
        brainrotHours = brainrot,
        midHours = mid,
        enrichmentHours = enrichment,
        totalHours = brainrot + mid + enrichment,
        roomObjectsPlaced = 0,
        startedAt = 0L,
        endedAt = 0L
    )

    @Test
    fun `dominantCategory returns None for zero hours`() {
        val record = makeRecord(0f, 0f, 0f)
        assertEquals("None", record.dominantCategory())
    }

    @Test
    fun `dominantCategory returns Brainrot when brainrot dominates`() {
        val record = makeRecord(brainrot = 70f, mid = 15f, enrichment = 15f)
        assertEquals("Brainrot", record.dominantCategory())
    }

    @Test
    fun `dominantCategory returns Mid when mid dominates`() {
        val record = makeRecord(brainrot = 15f, mid = 70f, enrichment = 15f)
        assertEquals("Mid", record.dominantCategory())
    }

    @Test
    fun `dominantCategory returns Enrichment when enrichment dominates`() {
        val record = makeRecord(brainrot = 15f, mid = 15f, enrichment = 70f)
        assertEquals("Enrichment", record.dominantCategory())
    }

    @Test
    fun `dominantCategory returns Hybrid when no category dominates`() {
        val record = makeRecord(brainrot = 34f, mid = 33f, enrichment = 33f)
        assertEquals("Hybrid", record.dominantCategory())
    }

    @Test
    fun `dominantCategory treats exactly 60 percent as not dominant`() {
        // 60% brainrot exactly — NOT > 0.6f
        val record = makeRecord(brainrot = 60f, mid = 20f, enrichment = 20f)
        assertEquals("Hybrid", record.dominantCategory())
    }
}
