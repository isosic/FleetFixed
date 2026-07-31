package com.isosic.fleetfixer.data

import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.flow.Flow

class BikeRepository(private val bikeDao: BikeDao) {

    fun observeBikes(): Flow<List<Bike>> = bikeDao.observeAll()

    suspend fun addBike(bike: Bike) {
        bikeDao.insert(bike)
    }
}
