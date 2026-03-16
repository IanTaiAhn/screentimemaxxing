package com.brainrotrpg

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end instrumented test for the lifecycle system.
 *
 * Tests the full lifecycle flow:
 * 1. Setting up mock stats near the XP threshold
 * 2. Triggering lifecycle completion via LifecycleEngine
 * 3. Confirming LifecycleRecord is written to DB with correct outcome
 * 4. Confirming pendingLifecycleEnd = true in PlayerStats
 * 5. Resetting stats via LifecycleEngine.resetStats (simulating "Begin New Life")
 * 6. Confirming stats are zeroed and lifecycle number increments
 * 7. Confirming archive shows completed lives in reverse chronological order
 * 8. Completing a second lifecycle and confirming both appear in archive
 */
@RunWith(AndroidJUnit4::class)
class LifecycleEndToEndTest {

    private lateinit var db: AppDatabase
    private lateinit var playerStatsDao: PlayerStatsDao
    private lateinit var lifecycleRecordDao: LifecycleRecordDao
    private lateinit var roomObjectDao: RoomObjectDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playerStatsDao = db.playerStatsDao()
        lifecycleRecordDao = db.lifecycleRecordDao()
        roomObjectDao = db.roomObjectDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ----------------------------
    // Helper: insert initial stats and simulate the worker detecting lifecycle completion
    // ----------------------------

    private suspend fun simulateLifecycleCompletion(
        brainrotHours: Float,
        midHours: Float,
        enrichmentHours: Float,
        lifecycleNumber: Int = 1,
        lifecycleStartedAt: Long = 1_000L,
        endedAt: Long = 99_000L
    ) {
        val totalXp = 16_000L
        val level = XpEngine.calculateLevel(totalXp)

        val statsForRecord = PlayerStats(
            id = 1,
            totalXp = totalXp,
            level = level,
            brainrotHours = brainrotHours,
            midHours = midHours,
            enrichmentHours = enrichmentHours,
            lastCheckedTimestamp = endedAt,
            lifecycleNumber = lifecycleNumber,
            lifecycleStartedAt = lifecycleStartedAt,
            pendingLifecycleEnd = false
        )

        val roomObjectsPlaced = roomObjectDao.getAll().size
        val record = LifecycleEngine.buildRecord(statsForRecord, roomObjectsPlaced, endedAt = endedAt)
        lifecycleRecordDao.insert(record)

        // Mark pendingLifecycleEnd so the UI can show the end screen
        playerStatsDao.upsert(statsForRecord.copy(pendingLifecycleEnd = true))
    }

    // ----------------------------
    // Step 1 & 2: Lifecycle detection and record creation
    // ----------------------------

    @Test
    fun step1_isLifecycleComplete_triggers_at_threshold() {
        // Simulate XP just at/above threshold (>160h equivalent = 16,000 XP)
        assertFalse(LifecycleEngine.isLifecycleComplete(15_999L))
        assertTrue(LifecycleEngine.isLifecycleComplete(16_000L))
        assertTrue(LifecycleEngine.isLifecycleComplete(16_001L))
    }

    @Test
    fun step2_lifecycleRecord_is_written_to_db_with_correct_outcome() = runBlocking {
        // Dominant brainrot → expect DIE outcome
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f)

        val records = lifecycleRecordDao.getAll()
        assertEquals(1, records.size)

        val record = records[0]
        assertEquals(LifecycleOutcome.DIE.name, record.outcome)
        assertEquals(1, record.lifecycleNumber)
        assertEquals(16_000L, record.totalXp)
        assertEquals(130f, record.brainrotHours, 0.001f)
        assertEquals(20f, record.midHours, 0.001f)
        assertEquals(10f, record.enrichmentHours, 0.001f)
        assertEquals(160f, record.totalHours, 0.001f)
    }

    // ----------------------------
    // Step 3: pendingLifecycleEnd = true after lifecycle completion
    // ----------------------------

    @Test
    fun step3_pendingLifecycleEnd_is_true_after_completion() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f)

        val stats = playerStatsDao.getStats()
        assertNotNull(stats)
        assertTrue(stats!!.pendingLifecycleEnd)
    }

    // ----------------------------
    // Steps 4 & 5: Simulating app relaunch — detecting pendingLifecycleEnd
    // and matching the end screen record to the DB record
    // ----------------------------

    @Test
    fun step4_app_relaunch_detects_pendingLifecycleEnd() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f)

        // On relaunch the app reads PlayerStats and routes to lifecycle_end if pending
        val stats = playerStatsDao.getStats()
        assertNotNull(stats)
        assertTrue("App should route to lifecycle_end screen", stats!!.pendingLifecycleEnd)
    }

    @Test
    fun step5_end_screen_record_matches_db_record() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 10f, midHours = 10f, enrichmentHours = 140f, endedAt = 55_000L)

        val mostRecent = lifecycleRecordDao.getAll().firstOrNull()
        assertNotNull(mostRecent)
        // The end screen would display this record
        assertEquals(LifecycleOutcome.GRADUATE.name, mostRecent!!.outcome)
        assertEquals(55_000L, mostRecent.endedAt)
        assertEquals(140f, mostRecent.enrichmentHours, 0.001f)
    }

    // ----------------------------
    // Step 6: "Begin New Life" — stats reset, room objects cleared
    // ----------------------------

    @Test
    fun step6_beginNewLife_resets_stats_and_clears_objects() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f)

        val statsBeforeReset = playerStatsDao.getStats()!!

        // Simulate "Begin New Life" tap — LifecycleViewModel calls resetStats + upsert
        val resetStats = LifecycleEngine.resetStats(statsBeforeReset, now = 200_000L)
        playerStatsDao.upsert(resetStats)
        roomObjectDao.deleteAll()

        val statsAfterReset = playerStatsDao.getStats()!!
        assertEquals(0L, statsAfterReset.totalXp)
        assertEquals(1, statsAfterReset.level)
        assertEquals(0f, statsAfterReset.brainrotHours, 0.001f)
        assertEquals(0f, statsAfterReset.midHours, 0.001f)
        assertEquals(0f, statsAfterReset.enrichmentHours, 0.001f)
        assertFalse(statsAfterReset.pendingLifecycleEnd)

        assertEquals(0, roomObjectDao.getAll().size)
    }

    // ----------------------------
    // Step 7: New life starts at Level 1 with incremented lifecycle number
    // ----------------------------

    @Test
    fun step7_new_life_starts_at_level_1_with_incremented_lifecycle_number() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f, lifecycleNumber = 1)

        val statsBeforeReset = playerStatsDao.getStats()!!
        val resetStats = LifecycleEngine.resetStats(statsBeforeReset, now = 200_000L)
        playerStatsDao.upsert(resetStats)

        val statsAfterReset = playerStatsDao.getStats()!!
        assertEquals(2, statsAfterReset.lifecycleNumber)
        assertEquals(1, statsAfterReset.level)
        assertEquals(0L, statsAfterReset.totalXp)
        assertEquals(200_000L, statsAfterReset.lifecycleStartedAt)
    }

    // ----------------------------
    // Step 8: Archive shows completed life as a card
    // ----------------------------

    @Test
    fun step8_archive_shows_completed_life() = runBlocking {
        simulateLifecycleCompletion(brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f)

        val records = lifecycleRecordDao.observeAll().first()
        assertEquals(1, records.size)

        val record = records[0]
        assertEquals(1, record.lifecycleNumber)
        assertEquals(LifecycleOutcome.DIE.name, record.outcome)
        assertEquals("Brainrot", record.dominantCategory())
    }

    // ----------------------------
    // Step 9: Second lifecycle — both lives in archive in reverse order
    // ----------------------------

    @Test
    fun step9_two_lifecycles_appear_in_archive_reverse_chronological() = runBlocking {
        // Complete first lifecycle (brainrot dominant → DIE)
        simulateLifecycleCompletion(
            brainrotHours = 130f, midHours = 20f, enrichmentHours = 10f,
            lifecycleNumber = 1, lifecycleStartedAt = 1_000L, endedAt = 50_000L
        )

        val statsAfterFirst = playerStatsDao.getStats()!!
        val resetToSecond = LifecycleEngine.resetStats(statsAfterFirst, now = 50_001L)
        playerStatsDao.upsert(resetToSecond)

        // Complete second lifecycle (enrichment dominant → GRADUATE)
        simulateLifecycleCompletion(
            brainrotHours = 10f, midHours = 20f, enrichmentHours = 130f,
            lifecycleNumber = 2, lifecycleStartedAt = 50_001L, endedAt = 100_000L
        )

        // Archive should list both lives, newest first (lifecycleNumber DESC)
        val records = lifecycleRecordDao.observeAll().first()
        assertEquals(2, records.size)

        // First item in list is the most recent (lifecycle 2)
        assertEquals(2, records[0].lifecycleNumber)
        assertEquals(LifecycleOutcome.GRADUATE.name, records[0].outcome)

        // Second item is the older life (lifecycle 1)
        assertEquals(1, records[1].lifecycleNumber)
        assertEquals(LifecycleOutcome.DIE.name, records[1].outcome)
    }
}
