package com.isosic.fleetfixer.data.repository

import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.data.local.BikeDao
import com.isosic.fleetfixer.data.remote.BikeRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BikeRepositoryImpl(
    private val bikeDao: BikeDao,
    private val remoteDataSource: BikeRemoteDataSource,
    private val appAuth: AppAuth,
    private val applicationScope: CoroutineScope
) : BikeRepository {

    private var syncJob: Job? = null

    override fun observeBikes(): Flow<List<Bike>> = bikeDao.observeAll()

    override fun observeBike(bikeId: String): Flow<Bike?> = bikeDao.observeById(bikeId)

    override fun startSync() {
        val uid = appAuth.currentUserId ?: return
        if (syncJob?.isActive == true) return
        syncJob = applicationScope.launch(Dispatchers.IO) {
            remoteDataSource.observeBikes(uid).collect { remoteBikes ->
                if (remoteBikes.isEmpty()) {
                    bikeDao.deleteAll()
                } else {
                    val localById = bikeDao.observeAll().first().associateBy { it.id }
                    val merged = remoteBikes.map { remote ->
                        mergePreservingLocalPurchaseDate(remote, localById[remote.id])
                    }
                    bikeDao.insertAll(merged)
                    bikeDao.deleteNotIn(merged.map { it.id })
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
        withContext(Dispatchers.IO) {
            remoteDataSource.upsertBike(uid, bike)
        }
    }

    override suspend fun replaceBikeId(oldBikeId: String, updatedBike: Bike) {
        require(oldBikeId != updatedBike.id) { "New bike id must differ from the old id" }
        bikeDao.insert(updatedBike)
        val uid = appAuth.currentUserId
        if (uid != null) {
            withContext(Dispatchers.IO) {
                remoteDataSource.upsertBike(uid, updatedBike)
                remoteDataSource.deleteBike(uid, oldBikeId)
            }
        }
        bikeDao.deleteById(oldBikeId)
    }

    override suspend fun addComponent(bikeId: String, component: BikeComponent) {
        upsertComponent(bikeId, component)
    }

    override suspend fun updateComponent(bikeId: String, component: BikeComponent) {
        upsertComponent(bikeId, component)
    }

    private suspend fun upsertComponent(bikeId: String, component: BikeComponent) {
        val bike = bikeDao.observeById(bikeId).first() ?: return
        val updated = bike.withComponent(component)
        bikeDao.insert(updated)
        val uid = appAuth.currentUserId ?: return
        withContext(Dispatchers.IO) {
            remoteDataSource.upsertBike(uid, updated)
        }
    }

    override suspend fun deleteBike(bikeId: String) {
        bikeDao.deleteById(bikeId)
        val uid = appAuth.currentUserId ?: return
        withContext(Dispatchers.IO) {
            remoteDataSource.deleteBike(uid, bikeId)
        }
    }

    override suspend fun clearLocalAndStopSync() {
        stopSync()
        bikeDao.deleteAll()
    }

    private fun mergePreservingLocalPurchaseDate(remote: Bike, local: Bike?): Bike {
        if (remote.purchaseDateEpochMillis != null || local?.purchaseDateEpochMillis == null) {
            return remote
        }
        return remote.copy(purchaseDateEpochMillis = local.purchaseDateEpochMillis)
    }
}
