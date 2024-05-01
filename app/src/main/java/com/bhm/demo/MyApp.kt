package com.bhm.demo

import com.bhm.demo.storage.DBHelper
import com.bhm.support.sdk.common.BaseApplication
import com.westingware.basemodule.storage.room.LocalDatabase

class MyApp : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        DBHelper.initDatabase(this, LocalDatabase::class.java)
    }

}