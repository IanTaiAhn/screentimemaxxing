package com.brainrotrpg

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoriesTest {

    @Test
    fun `TikTok package returns BRAINROT`() {
        assertEquals(Category.BRAINROT, getCategory("com.zhiliaoapp.musically"))
    }

    @Test
    fun `Instagram package returns BRAINROT`() {
        assertEquals(Category.BRAINROT, getCategory("com.instagram.android"))
    }

    @Test
    fun `YouTube package returns MID`() {
        assertEquals(Category.MID, getCategory("com.google.android.youtube"))
    }

    @Test
    fun `Twitter package returns MID`() {
        assertEquals(Category.MID, getCategory("com.twitter.android"))
    }

    @Test
    fun `Reddit package returns MID`() {
        assertEquals(Category.MID, getCategory("com.reddit.frontpage"))
    }

    @Test
    fun `Spotify package returns ENRICHMENT`() {
        assertEquals(Category.ENRICHMENT, getCategory("com.spotify.music"))
    }

    @Test
    fun `Audible package returns ENRICHMENT`() {
        assertEquals(Category.ENRICHMENT, getCategory("com.audible.application"))
    }

    @Test
    fun `Google Podcasts package returns ENRICHMENT`() {
        assertEquals(Category.ENRICHMENT, getCategory("com.google.android.apps.podcasts"))
    }

    @Test
    fun `Unknown package returns UNTRACKED`() {
        assertEquals(Category.UNTRACKED, getCategory("com.unknown.app"))
    }
}
