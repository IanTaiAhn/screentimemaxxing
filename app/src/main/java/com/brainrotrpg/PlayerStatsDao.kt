package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlayerStatsDao {

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getStats(): PlayerStats?

    @Upsert
    suspend fun upsert(stats: PlayerStats)
}
