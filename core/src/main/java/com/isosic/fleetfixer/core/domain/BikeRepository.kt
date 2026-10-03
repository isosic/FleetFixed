package com.isosic.fleetfixer.core.domain

import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import kotlinx.coroutines.flow.Flow

interface BikeRepository {
    fun observeBikes(): Flow<List<Bike>>
    fun observeBike(bikeId: String): Flow<Bike?>
    fun startSync()
    fun stopSync()
    suspend fun addBike(bike: Bike)
    suspend fun replaceBikeId(oldBikeId: String, updatedBike: Bike)
    suspend fun addComponent(bikeId: String, component: BikeComponent)
    suspend fun updateComponent(bikeId: String, component: BikeComponent)
    suspend fun deleteBike(bikeId: String)
    suspend fun clearLocalAndStopSync()
}
