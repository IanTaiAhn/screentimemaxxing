package com.brainrotrpg

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS room_objects (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                worldX REAL NOT NULL,
                worldY REAL NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 0,
                activatedAt INTEGER NOT NULL DEFAULT 0,
                activeDurationMs INTEGER NOT NULL DEFAULT 14400000
            )
        """)
        database.execSQL("ALTER TABLE player_stats ADD COLUMN spendableBrainrotHours REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE player_stats ADD COLUMN spendableMidHours REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE player_stats ADD COLUMN spendableEnrichmentHours REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS lifecycle_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lifecycleNumber INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                finalAvatarClass TEXT NOT NULL,
                totalXp INTEGER NOT NULL,
                finalLevel INTEGER NOT NULL,
                brainrotHours REAL NOT NULL,
                midHours REAL NOT NULL,
                enrichmentHours REAL NOT NULL,
                totalHours REAL NOT NULL,
                roomObjectsPlaced INTEGER NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL
            )
        """)
        // Track when the current life started (default to 0 for existing installs)
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lifecycleNumber INTEGER NOT NULL DEFAULT 1"
        )
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lifecycleStartedAt INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN pendingLifecycleEnd INTEGER NOT NULL DEFAULT 0"
        )
    }
}

object DatabaseProvider {
    private const val DATABASE_NAME = "brainrot_rpg.db"

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build().also { instance = it }
        }
    }
}
