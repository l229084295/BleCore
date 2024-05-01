package com.bhm.demo.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * DBHelper
 * @date: 2021/7/23 18:04
 * @author: renyihao
 * @desc: 数据库工具
 */
object DBHelper {

    private var database: RoomDatabase? = null

    fun <DB : RoomDatabase> initDatabase(context: Context?, dbClass: Class<DB>) {
        if (context == null) throw RuntimeException("Context Null")
        val builder = Room.databaseBuilder(context, dbClass, DBConstants.DB_NAME)
        database = builder.build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <DB : RoomDatabase> getDataBase(): DB {
        return database!! as DB
    }
}