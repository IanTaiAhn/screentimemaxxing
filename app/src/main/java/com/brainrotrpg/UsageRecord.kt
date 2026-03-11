package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val category: String, // Stores Category.name (e.g. "BRAINROT", "MID", "ENRICHMENT")
    val durationMillis: Long
)
