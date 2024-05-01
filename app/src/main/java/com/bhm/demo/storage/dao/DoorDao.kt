package com.bhm.demo.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bhm.demo.storage.entity.Door

@Dao
interface DoorDao {

    @Query("select * from Door")
    suspend fun getDoors(): List<Door>

    @Insert
    suspend fun insertData(vararg data: Door)

    @Delete
    suspend fun delete(vararg data: Door)
}