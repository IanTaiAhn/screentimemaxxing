package com.brainrotrpg

enum class Category {
    BRAINROT,
    MID,
    ENRICHMENT,
    UNTRACKED
}

val packageCategoryMap: Map<String, Category> = mapOf(
    "com.zhiliaoapp.musically" to Category.BRAINROT,       // TikTok
    "com.instagram.android" to Category.BRAINROT,          // Instagram

    "com.google.android.youtube" to Category.MID,          // YouTube
    "com.twitter.android" to Category.MID,                 // Twitter/X
    "com.reddit.frontpage" to Category.MID,                // Reddit

    "com.spotify.music" to Category.ENRICHMENT,            // Spotify
    "com.audible.application" to Category.ENRICHMENT,      // Audible
    "com.google.android.apps.podcasts" to Category.ENRICHMENT  // Google Podcasts
)

fun getCategory(packageName: String): Category =
    packageCategoryMap[packageName] ?: Category.UNTRACKED
