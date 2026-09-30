package com.pokerlanka.pirith

import android.app.Application
import com.pokerlanka.pirith.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PirithApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PirithApp)
            modules(appModule)
        }
    }
}
