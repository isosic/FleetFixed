package com.isosic.fleetfixer.feature.auth.di

import com.isosic.fleetfixer.feature.auth.ui.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authFeatureModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
}
