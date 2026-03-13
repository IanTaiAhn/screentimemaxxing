package com.brainrotrpg

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UsageRecord::class, PlayerStats::class, RoomObject::class],
    version = 2,           // bumped from 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun roomObjectDao(): RoomObjectDao
}
