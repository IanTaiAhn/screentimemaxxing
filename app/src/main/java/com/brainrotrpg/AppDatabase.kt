package com.brainrotrpg

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UsageRecord::class, PlayerStats::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
}
