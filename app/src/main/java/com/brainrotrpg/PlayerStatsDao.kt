package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getStats(): PlayerStats?

    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun observeStats(): Flow<PlayerStats?>

    @Upsert
    suspend fun upsert(stats: PlayerStats)
}
