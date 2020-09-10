package com.kk.hardware.demo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

/**
 * 程序 Application
 * @author zhangtao
 * @date 2020/6/12 10:13
 */
class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this) // 65535
    }
}