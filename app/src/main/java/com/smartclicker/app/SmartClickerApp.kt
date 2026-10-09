package com.smartclicker.app

import android.app.Application
import android.content.Context
import com.smartclicker.app.utils.LocaleHelper

class SmartClickerApp : Application() {

    override fun attachBaseContext(base: Context) {
        val lang = LocaleHelper.getSavedLanguage(base)
        super.attachBaseContext(LocaleHelper.applyLanguage(base, lang))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.smartclicker.app.utils.CrashLogger.init(this)
    }

    companion object {
        lateinit var instance: SmartClickerApp
            private set
    }
}
