package com.bhm.demo

import android.content.Context
import com.bhm.demo.storage.DBHelper
import com.bhm.support.sdk.common.BaseApplication
import com.blankj.utilcode.util.ToastUtils
import com.westingware.basemodule.storage.room.LocalDatabase
import xcrash.ICrashCallback
import xcrash.XCrash


class MyApp : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        DBHelper.initDatabase(this, LocalDatabase::class.java)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        val param = XCrash.InitParameters()
            .setJavaRethrow(true)
            .setJavaLogCountMax(10)
            .setJavaDumpAllThreadsWhiteList(arrayOf("^main$", "^Binder:.*", ".*Finalizer.*"))
            .setJavaDumpAllThreadsCountMax(10)
            .setJavaCallback(callback)
            .setNativeRethrow(true)
            .setNativeLogCountMax(10)
            .setNativeDumpAllThreadsWhiteList(arrayOf("^xcrash\\.sample$", "^Signal Catcher$", "^Jit thread pool$", ".*(R|r)ender.*", ".*Chrome.*"))
            .setNativeDumpAllThreadsCountMax(10)
            .setAnrRethrow(true)
            .setAnrLogCountMax(10)
            .setPlaceholderCountMax(3)
            .setPlaceholderSizeKb(512)
            .setLogFileMaintainDelayMs(1000)
        XCrash.init(this, param)
    }

    private var callback: ICrashCallback = ICrashCallback { logPath, emergency ->
        ToastUtils.showShort("log path: " + (logPath ?: "(null)") + ", emergency: " + (emergency ?: "(null)"))
    }

}