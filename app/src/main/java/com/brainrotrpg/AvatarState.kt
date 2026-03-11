package com.brainrotrpg

sealed class AvatarState {
    object SigmaZombie : AvatarState()       // dominant BRAINROT (>60% of hours)
    object ExtremelyOnline : AvatarState()   // dominant MID (>60% of hours)
    object FakeIntellectual : AvatarState()  // dominant ENRICHMENT (>60% of hours)
    object Hybrid : AvatarState()            // no dominant category
}

fun resolveAvatarState(brainrotHours: Float, midHours: Float, enrichmentHours: Float): AvatarState {
    val total = brainrotHours + midHours + enrichmentHours
    if (total == 0f) return AvatarState.Hybrid

    val brainrotPct = brainrotHours / total
    val midPct = midHours / total
    val enrichmentPct = enrichmentHours / total

    return when {
        brainrotPct > 0.6f -> AvatarState.SigmaZombie
        midPct > 0.6f -> AvatarState.ExtremelyOnline
        enrichmentPct > 0.6f -> AvatarState.FakeIntellectual
        else -> AvatarState.Hybrid
    }
}
