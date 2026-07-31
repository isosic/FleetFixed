package com.isosic.fleetfixer.di

import androidx.room.Room
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.data.FleetFixerDatabase
import com.isosic.fleetfixer.screens.addbike.AddBikeViewModel
import com.isosic.fleetfixer.screens.homescreen.HomeScreenViewModel
import com.isosic.fleetfixer.screens.login.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AuthTokenStore(get()) }
    single { GoogleAuthClient(get()) }

    single {
        Room.databaseBuilder(
            get(),
            FleetFixerDatabase::class.java,
            "fleetfixer.db"
        ).build()
    }
    single { get<FleetFixerDatabase>().bikeDao() }
    single { BikeRepository(get()) }

    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeScreenViewModel(get(), get(), get()) }
    viewModel { AddBikeViewModel(get()) }
}
