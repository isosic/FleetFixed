package com.isosic.fleetfixer.di

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.BikeRemoteDataSource
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.data.FleetFixerDatabase
import com.isosic.fleetfixer.screens.addbike.AddBikeViewModel
import com.isosic.fleetfixer.screens.bikedetail.BikeDetailViewModel
import com.isosic.fleetfixer.screens.homescreen.HomeScreenViewModel
import com.isosic.fleetfixer.screens.login.LoginViewModel
import com.isosic.fleetfixer.strava.StravaApiClient
import com.isosic.fleetfixer.strava.StravaAuthClient
import com.isosic.fleetfixer.strava.StravaTokenStore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AuthTokenStore(get()) }
    single { GoogleAuthClient(get()) }
    single { StravaTokenStore(get()) }
    single { StravaAuthClient(get()) }
    single { StravaApiClient(get()) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single {
        Room.databaseBuilder(
            get(),
            FleetFixerDatabase::class.java,
            "fleetfixer.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<FleetFixerDatabase>().bikeDao() }
    single { BikeRemoteDataSource(get()) }
    single { BikeRepository(get(), get(), get()) }

    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { HomeScreenViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AddBikeViewModel(get()) }
    viewModel { BikeDetailViewModel(get(), get()) }
}
