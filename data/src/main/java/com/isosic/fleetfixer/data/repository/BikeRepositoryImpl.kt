package com.isosic.fleetfixer.data.repository

import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.data.local.BikeDao
import com.isosic.fleetfixer.data.remote.BikeRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BikeRepositoryImpl(
    private val bikeDao: BikeDao,
    private val remoteDataSource: BikeRemoteDataSource,
    private val appAuth: AppAuth
) : BikeRepository {

    private var syncJob: Job? = null

    override fun observeBikes(): Flow<List<Bike>> = bikeDao.observeAll()

    override fun observeBike(bikeId: String): Flow<Bike?> = bikeDao.observeById(bikeId)

    override fun startSync(scope: CoroutineScope) {
        val uid = appAuth.currentUserId ?: return
        syncJob?.cancel()
        syncJob = scope.launch {
            remoteDataSource.observeBikes(uid).collect { bikes ->
                if (bikes.isEmpty()) {
                    bikeDao.deleteAll()
                } else {
                    bikeDao.insertAll(bikes)
                    bikeDao.deleteNotIn(bikes.map { it.id })
                }
            }
        }
    }

    override fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    override suspend fun addBike(bike: Bike) {
        bikeDao.insert(bike)
        val uid = appAuth.currentUserId ?: return
        remoteDataSource.upsertBike(uid, bike)
    }

    override suspend fun replaceBikeId(oldBikeId: String, updatedBike: Bike) {
        require(oldBikeId != updatedBike.id) { "New bike id must differ from the old id" }
        bikeDao.insert(updatedBike)
        val uid = appAuth.currentUserId
        if (uid != null) {
            remoteDataSource.upsertBike(uid, updatedBike)
            remoteDataSource.deleteBike(uid, oldBikeId)
        }
        bikeDao.deleteById(oldBikeId)
    }

    override suspend fun addComponent(bikeId: String, component: BikeComponent) {
        val bike = bikeDao.observeById(bikeId).first() ?: return
        val updated = bike.withComponent(component)
        bikeDao.insert(updated)
        val uid = appAuth.currentUserId ?: return
        remoteDataSource.upsertBike(uid, updated)
    }

    override suspend fun deleteBike(bikeId: String) {
        bikeDao.deleteById(bikeId)
        val uid = appAuth.currentUserId ?: return
        remoteDataSource.deleteBike(uid, bikeId)
    }

    override suspend fun clearLocalAndStopSync() {
        stopSync()
        bikeDao.deleteAll()
    }
}
