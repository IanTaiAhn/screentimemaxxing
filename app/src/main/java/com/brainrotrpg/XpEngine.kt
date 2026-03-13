package com.brainrotrpg

object XpEngine {

    private const val XP_PER_HOUR = 100L

    private val LEVEL_THRESHOLDS = listOf(
        0L,    // Level 1
        500L,  // Level 2
        1200L, // Level 3
        2000L, // Level 4
        3000L, // Level 5
        4500L, // Level 6
        6500L, // Level 7
        9000L, // Level 8
        12000L,// Level 9
        16000L // Level 10
    )

    fun calculateXp(brainrotHours: Float, midHours: Float, enrichmentHours: Float): Long {
        val totalHours = brainrotHours + midHours + enrichmentHours
        return (totalHours * XP_PER_HOUR).toLong()
    }

    fun calculateXpWithMultipliers(
        brainrotHours: Float,
        midHours: Float,
        enrichmentHours: Float,
        brainrotMultiplier: Float = 1f,
        midMultiplier: Float = 1f,
        enrichmentMultiplier: Float = 1f
    ): Long {
        val xp = (brainrotHours * XP_PER_HOUR * brainrotMultiplier) +
                  (midHours * XP_PER_HOUR * midMultiplier) +
                  (enrichmentHours * XP_PER_HOUR * enrichmentMultiplier)
        return xp.toLong()
    }

    fun calculateLevel(totalXp: Long): Int {
        var level = 1
        for (i in LEVEL_THRESHOLDS.indices) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) {
                level = i + 1
            } else {
                break
            }
        }
        return level
    }

    fun xpToNextLevel(totalXp: Long): Long {
        val level = calculateLevel(totalXp)
        return if (level >= LEVEL_THRESHOLDS.size) 0L
        else LEVEL_THRESHOLDS[level] - totalXp
    }

    fun currentLevelThreshold(totalXp: Long): Long {
        val level = calculateLevel(totalXp)
        return LEVEL_THRESHOLDS[level - 1]
    }
}
