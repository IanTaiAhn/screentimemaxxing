package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LifecycleRecordDao {

    @Insert
    suspend fun insert(record: LifecycleRecord): Long

    @Query("SELECT * FROM lifecycle_records ORDER BY lifecycleNumber DESC")
    fun observeAll(): Flow<List<LifecycleRecord>>

    @Query("SELECT * FROM lifecycle_records ORDER BY lifecycleNumber DESC")
    suspend fun getAll(): List<LifecycleRecord>

    @Query("SELECT COUNT(*) FROM lifecycle_records")
    suspend fun getCount(): Int

    @Query("SELECT * FROM lifecycle_records WHERE id = :id")
    suspend fun getById(id: Long): LifecycleRecord?
}
