package com.isosic.fleetfixer.di

import com.isosic.fleetfixer.data.di.dataModule
import com.isosic.fleetfixer.feature.auth.di.authFeatureModule
import com.isosic.fleetfixer.feature.bikes.di.bikesFeatureModule
import com.isosic.fleetfixer.feature.strava.di.stravaFeatureModule

val appModules = listOf(
    dataModule,
    authFeatureModule,
    bikesFeatureModule,
    stravaFeatureModule
)
