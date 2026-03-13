package com.brainrotrpg

enum class LifecycleOutcome(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    GRADUATE(
        displayName = "Graduate",
        emoji = "🎓",
        description = "You consumed enough culture to escape the feed."
    ),
    DIE(
        displayName = "Die",
        emoji = "💀",
        description = "The brainrot consumed you. There was nothing left."
    ),
    ASCEND(
        displayName = "Ascend",
        emoji = "✨",
        description = "Extremely online. Deeply chronically. You became the internet."
    )
}

fun resolveLifecycleOutcome(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float
): LifecycleOutcome {
    val total = brainrotHours + midHours + enrichmentHours
    if (total == 0f) return LifecycleOutcome.ASCEND

    val brainrotPct = brainrotHours / total
    val enrichmentPct = enrichmentHours / total

    return when {
        enrichmentPct > 0.6f -> LifecycleOutcome.GRADUATE
        brainrotPct > 0.6f -> LifecycleOutcome.DIE
        else -> LifecycleOutcome.ASCEND
    }
}
