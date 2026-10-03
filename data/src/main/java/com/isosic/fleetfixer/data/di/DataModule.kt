package com.isosic.fleetfixer.data.di

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.AuthTokenStore
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.domain.GoogleSignInGateway
import com.isosic.fleetfixer.core.domain.SelectedBikeStore
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.domain.StravaBikeRemoteSource
import com.isosic.fleetfixer.core.domain.StravaTokenStore
import com.isosic.fleetfixer.data.auth.AuthTokenStoreImpl
import com.isosic.fleetfixer.data.auth.FirebaseAppAuth
import com.isosic.fleetfixer.data.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.local.FleetFixerDatabase
import com.isosic.fleetfixer.data.local.SelectedBikeStoreImpl
import com.isosic.fleetfixer.data.remote.BikeRemoteDataSource
import com.isosic.fleetfixer.data.repository.BikeRepositoryImpl
import com.isosic.fleetfixer.data.strava.StravaApiClient
import com.isosic.fleetfixer.data.strava.StravaAuthClient
import com.isosic.fleetfixer.data.strava.StravaTokenStoreImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<AuthTokenStore> { AuthTokenStoreImpl(androidContext()) }
    single<SelectedBikeStore> { SelectedBikeStoreImpl(androidContext()) }
    single<GoogleSignInGateway> { GoogleAuthClient(androidContext()) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AppAuth> { FirebaseAppAuth(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            FleetFixerDatabase::class.java,
            "fleetfixer.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<FleetFixerDatabase>().bikeDao() }
    single { BikeRemoteDataSource(get()) }
    single<BikeRepository> { BikeRepositoryImpl(get(), get(), get()) }

    single<StravaTokenStore> { StravaTokenStoreImpl(androidContext()) }
    single<StravaAuthRepository> { StravaAuthClient(get()) }
    single<StravaBikeRemoteSource> { StravaApiClient(get()) }
}
