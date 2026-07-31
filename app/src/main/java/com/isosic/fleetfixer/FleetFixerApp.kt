package com.isosic.fleetfixer

import android.app.Application
import com.isosic.fleetfixer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FleetFixerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@FleetFixerApp)
            modules(appModule)
        }
    }
}
