package com.isosic.fleetfixer.feature.bikes.di

import com.isosic.fleetfixer.feature.bikes.addbike.AddBikeViewModel
import com.isosic.fleetfixer.feature.bikes.detail.BikeDetailViewModel
import com.isosic.fleetfixer.feature.bikes.home.HomeScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bikesFeatureModule = module {
    viewModel { HomeScreenViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AddBikeViewModel(get()) }
    viewModel { BikeDetailViewModel(get(), get()) }
}
