package com.bhm.demo.vm

import android.app.Application
import com.bhm.demo.storage.DBHelper
import com.bhm.support.sdk.common.BaseViewModel
import com.westingware.basemodule.storage.room.LocalDatabase

open class MyBaseViewModel(private val app: Application) : BaseViewModel(app) {
    val dbRepository by lazy { DBHelper.getDataBase<LocalDatabase>() }

}