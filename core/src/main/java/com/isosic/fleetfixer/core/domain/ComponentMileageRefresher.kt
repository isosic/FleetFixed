package com.isosic.fleetfixer.core.domain

interface ComponentMileageRefresher {
    suspend fun updateAllBikesWithComponents(): Result<Unit>
    suspend fun updateBike(bikeId: String): Result<Unit>
}
