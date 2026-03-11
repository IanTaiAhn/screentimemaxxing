package com.brainrotrpg

import org.junit.Assert.assertEquals
import org.junit.Test

class XpEngineTest {

    // calculateXp tests

    @Test
    fun `calculateXp returns 0 for all zero hours`() {
        assertEquals(0L, XpEngine.calculateXp(0f, 0f, 0f))
    }

    @Test
    fun `calculateXp returns 100 for one hour total`() {
        assertEquals(100L, XpEngine.calculateXp(1f, 0f, 0f))
        assertEquals(100L, XpEngine.calculateXp(0f, 1f, 0f))
        assertEquals(100L, XpEngine.calculateXp(0f, 0f, 1f))
    }

    @Test
    fun `calculateXp sums all categories equally`() {
        // 3h brainrot + 1h enrichment = 4 hours = 400 XP
        assertEquals(400L, XpEngine.calculateXp(3f, 0f, 1f))
    }

    @Test
    fun `calculateXp handles mixed hours`() {
        // 2h brainrot + 1h mid + 1h enrichment = 4 hours = 400 XP
        assertEquals(400L, XpEngine.calculateXp(2f, 1f, 1f))
    }

    @Test
    fun `calculateXp handles fractional hours`() {
        // 0.5h = 50 XP
        assertEquals(50L, XpEngine.calculateXp(0.5f, 0f, 0f))
    }

    // calculateLevel tests

    @Test
    fun `calculateLevel returns 1 for 0 XP`() {
        assertEquals(1, XpEngine.calculateLevel(0L))
    }

    @Test
    fun `calculateLevel returns 1 for XP below level 2 threshold`() {
        assertEquals(1, XpEngine.calculateLevel(499L))
    }

    @Test
    fun `calculateLevel returns 2 at 500 XP`() {
        assertEquals(2, XpEngine.calculateLevel(500L))
    }

    @Test
    fun `calculateLevel returns 3 at 1200 XP`() {
        assertEquals(3, XpEngine.calculateLevel(1200L))
    }

    @Test
    fun `calculateLevel returns 4 at 2000 XP`() {
        assertEquals(4, XpEngine.calculateLevel(2000L))
    }

    @Test
    fun `calculateLevel returns 5 at 3000 XP`() {
        assertEquals(5, XpEngine.calculateLevel(3000L))
    }

    @Test
    fun `calculateLevel returns correct level just below threshold`() {
        assertEquals(2, XpEngine.calculateLevel(1199L))
        assertEquals(3, XpEngine.calculateLevel(1999L))
        assertEquals(4, XpEngine.calculateLevel(2999L))
    }

    @Test
    fun `calculateLevel returns max level for very high XP`() {
        assertEquals(10, XpEngine.calculateLevel(16000L))
        assertEquals(10, XpEngine.calculateLevel(999999L))
    }
}
