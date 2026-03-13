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
            .addMigrations(MIGRATION_1_2)
            .build().also { instance = it }
        }
    }
}
