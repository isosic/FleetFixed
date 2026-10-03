package com.isosic.fleetfixer.core.domain

import kotlinx.coroutines.flow.Flow

interface SelectedBikeStore {
    val selectedBikeId: Flow<String?>
    suspend fun select(bikeId: String)
    suspend fun clear()
}
