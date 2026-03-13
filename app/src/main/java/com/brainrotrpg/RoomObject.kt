package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_objects")
data class RoomObject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,               // RoomObjectType.name — stored as string for Room compatibility
    val worldX: Float,              // normalised world position (0f..1f)
    val worldY: Float,
    val isActive: Boolean = false,
    val activatedAt: Long = 0L,     // timestamp of last tap activation
    val activeDurationMs: Long = 4 * 60 * 60 * 1000L  // 4 hours default
)

fun RoomObject.objectType(): RoomObjectType = RoomObjectType.valueOf(type)

fun RoomObject.isCurrentlyActive(now: Long = System.currentTimeMillis()): Boolean {
    return isActive && (activatedAt + activeDurationMs) > now
}

fun RoomObject.timeRemainingMs(now: Long = System.currentTimeMillis()): Long {
    return if (isCurrentlyActive(now)) (activatedAt + activeDurationMs) - now else 0L
}
