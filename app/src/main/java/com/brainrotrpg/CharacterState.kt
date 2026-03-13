package com.brainrotrpg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sqrt

class CharacterState {

    // Current rendered position in normalised world space (0f..1f)
    var worldX by mutableStateOf(0.5f)
    var worldY by mutableStateOf(0.5f)

    // Where the character is walking toward
    var targetX by mutableStateOf(0.5f)
    var targetY by mutableStateOf(0.5f)

    var isMoving by mutableStateOf(false)

    /**
     * Called when the player taps the floor.
     * [x] and [y] are normalised world coordinates (0f..1f).
     */
    fun setTarget(x: Float, y: Float) {
        targetX = x.coerceIn(0.05f, 0.95f)
        targetY = y.coerceIn(0.05f, 0.95f)
        isMoving = true
    }

    /**
     * Advance the character position toward the target.
     * Called once per frame; [deltaMs] is elapsed milliseconds since last frame.
     *
     * Speed is expressed in world-units per second. 1.2f feels natural for the
     * default room size — increase for a snappier feel, decrease for a slower walk.
     */
    fun tick(deltaMs: Float) {
        val speed = (deltaMs / 1000f) * 1.2f
        val dx = targetX - worldX
        val dy = targetY - worldY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < 0.008f) {
            worldX = targetX
            worldY = targetY
            isMoving = false
        } else {
            val move = minOf(speed, dist)
            worldX += (dx / dist) * move
            worldY += (dy / dist) * move
        }
    }
}
