package com.brainrotrpg

data class XpMultipliers(
    val brainrot: Float = 1f,
    val mid: Float = 1f,
    val enrichment: Float = 1f
)

enum class RoomObjectType(
    val displayName: String,
    val emoji: String,
    val costCategory: Category,
    val costHours: Float,
    val multipliers: XpMultipliers,
    val description: String
) {
    WEIGHTS(
        displayName = "Weights",
        emoji = "🏋️",
        costCategory = Category.ENRICHMENT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.15f),
        description = "+15% all XP while active"
    ),
    BOOKSHELF(
        displayName = "Bookshelf",
        emoji = "📚",
        costCategory = Category.ENRICHMENT,
        costHours = 3f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.25f),
        description = "+25% Enrichment XP while active"
    ),
    SECOND_MONITOR(
        displayName = "Second Monitor",
        emoji = "🖥️",
        costCategory = Category.MID,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1.20f, enrichment = 1f),
        description = "+20% Mid XP while active"
    ),
    PIZZA_TOWER(
        displayName = "Pizza Tower",
        emoji = "🍕",
        costCategory = Category.BRAINROT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1.20f, mid = 1f, enrichment = 1f),
        description = "+20% Brainrot XP while active"
    ),
    ENERGY_FRIDGE(
        displayName = "Energy Fridge",
        emoji = "⚡",
        costCategory = Category.BRAINROT,
        costHours = 3f,
        multipliers = XpMultipliers(brainrot = 1.15f, mid = 1.15f, enrichment = 0.90f),
        description = "+15% Brainrot & Mid XP, -10% Enrichment while active"
    ),
    HEADPHONE_STAND(
        displayName = "Headphone Stand",
        emoji = "🎧",
        costCategory = Category.ENRICHMENT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.20f),
        description = "+20% Enrichment XP while active"
    )
}
