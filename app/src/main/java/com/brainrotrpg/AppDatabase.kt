package com.brainrotrpg

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UsageRecord::class, PlayerStats::class, RoomObject::class, LifecycleRecord::class],
    version = 3,           // bumped from 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun roomObjectDao(): RoomObjectDao
    abstract fun lifecycleRecordDao(): LifecycleRecordDao
}
