package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomObjectDao {

    @Insert
    suspend fun insert(obj: RoomObject): Long

    @Update
    suspend fun update(obj: RoomObject)

    @Delete
    suspend fun delete(obj: RoomObject)

    @Query("SELECT * FROM room_objects")
    fun observeAll(): Flow<List<RoomObject>>

    @Query("SELECT * FROM room_objects")
    suspend fun getAll(): List<RoomObject>

    @Query("SELECT * FROM room_objects WHERE id = :id")
    suspend fun getById(id: Long): RoomObject?

    // Returns all objects where the active window has not yet expired
    @Query("SELECT * FROM room_objects WHERE isActive = 1 AND (activatedAt + activeDurationMs) > :now")
    suspend fun getActiveObjects(now: Long = System.currentTimeMillis()): List<RoomObject>
}
