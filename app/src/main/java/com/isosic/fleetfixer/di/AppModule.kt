package com.isosic.fleetfixer.di

import com.isosic.fleetfixer.core.domain.PendingWorkNotifier
import com.isosic.fleetfixer.data.di.dataModule
import com.isosic.fleetfixer.feature.auth.di.authFeatureModule
import com.isosic.fleetfixer.feature.bikes.di.bikesFeatureModule
import com.isosic.fleetfixer.feature.strava.di.stravaFeatureModule
import com.isosic.fleetfixer.notifications.PendingWorkNotifierImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val appModule = module {
    single<PendingWorkNotifier> {
        PendingWorkNotifierImpl(
            context = androidContext(),
            applicationScope = get()
        )
    }
}

val appModules = listOf(
    dataModule,
    appModule,
    authFeatureModule,
    bikesFeatureModule,
    stravaFeatureModule
)
