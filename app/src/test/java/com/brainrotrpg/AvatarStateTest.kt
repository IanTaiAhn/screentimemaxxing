package com.brainrotrpg

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarStateTest {

    @Test
    fun `resolveAvatarState returns SigmaZombie when brainrot exceeds 60 percent`() {
        assertEquals(AvatarState.SigmaZombie, resolveAvatarState(7f, 2f, 1f))
    }

    @Test
    fun `resolveAvatarState returns SigmaZombie when brainrot is exactly above 60 percent`() {
        assertEquals(AvatarState.SigmaZombie, resolveAvatarState(61f, 39f, 0f))
    }

    @Test
    fun `resolveAvatarState returns ExtremelyOnline when mid exceeds 60 percent`() {
        assertEquals(AvatarState.ExtremelyOnline, resolveAvatarState(1f, 8f, 1f))
    }

    @Test
    fun `resolveAvatarState returns ExtremelyOnline when mid is exactly above 60 percent`() {
        assertEquals(AvatarState.ExtremelyOnline, resolveAvatarState(0f, 61f, 39f))
    }

    @Test
    fun `resolveAvatarState returns FakeIntellectual when enrichment exceeds 60 percent`() {
        assertEquals(AvatarState.FakeIntellectual, resolveAvatarState(1f, 2f, 7f))
    }

    @Test
    fun `resolveAvatarState returns FakeIntellectual when enrichment is exactly above 60 percent`() {
        assertEquals(AvatarState.FakeIntellectual, resolveAvatarState(0f, 39f, 61f))
    }

    @Test
    fun `resolveAvatarState returns Hybrid when no category dominates`() {
        assertEquals(AvatarState.Hybrid, resolveAvatarState(4f, 3f, 3f))
    }

    @Test
    fun `resolveAvatarState returns Hybrid for perfectly even split`() {
        assertEquals(AvatarState.Hybrid, resolveAvatarState(1f, 1f, 1f))
    }

    @Test
    fun `resolveAvatarState returns Hybrid when total hours is zero`() {
        assertEquals(AvatarState.Hybrid, resolveAvatarState(0f, 0f, 0f))
    }

    @Test
    fun `resolveAvatarState returns Hybrid when brainrot is exactly 60 percent (not above)`() {
        // 60% is not strictly greater than 60%, so should be Hybrid
        assertEquals(AvatarState.Hybrid, resolveAvatarState(6f, 2f, 2f))
    }
}
