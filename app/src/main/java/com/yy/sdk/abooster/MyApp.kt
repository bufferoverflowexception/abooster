package com.yy.sdk.abooster

import android.app.Application
import android.content.Context
import com.yy.sdk.installer.LibABooster

/**
 * Create by nls on 2022/7/21
 * description: MyApp
 */
class MyApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        LibABooster.initialize(this)
    }

    override fun onCreate() {
        super.onCreate()

    }
}