package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageRecordDao {

    @Insert
    suspend fun insert(record: UsageRecord)

    @Query("SELECT * FROM usage_records WHERE timestamp >= :timestamp")
    suspend fun getRecordsSince(timestamp: Long): List<UsageRecord>

    @Query("DELETE FROM usage_records WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
