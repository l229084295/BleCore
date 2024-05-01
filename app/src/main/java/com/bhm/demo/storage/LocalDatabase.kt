package com.westingware.basemodule.storage.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.bhm.demo.storage.DBConstants
import com.bhm.demo.storage.dao.DoorDao
import com.bhm.demo.storage.entity.Door

/**
 * TeacherDatabase
 * @date: 2021/7/23 18:45
 * @author: renyihao
 * @desc: 本地数据库
 */
@Database(
    entities = [
        Door::class
    ],
//    autoMigrations = [
//        AutoMigration(from = 1, to = 2)
//    ],
    version = DBConstants.DB_VERSION,
    exportSchema = true
)
abstract class LocalDatabase : RoomDatabase() {


    /**步数**/
    abstract fun DoorDao(): DoorDao

}