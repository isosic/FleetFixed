package com.isosic.fleetfixer.data

import com.google.firebase.auth.FirebaseAuth
import com.isosic.fleetfixer.models.Bike
import com.isosic.fleetfixer.models.BikeComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BikeRepository(
    private val bikeDao: BikeDao,
    private val remoteDataSource: BikeRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
) {

    private var syncJob: Job? = null

    fun observeBikes(): Flow<List<Bike>> = bikeDao.observeAll()

    fun observeBike(bikeId: String): Flow<Bike?> = bikeDao.observeById(bikeId)

    fun startSync(scope: CoroutineScope) {
        val uid = firebaseAuth.currentUser?.uid ?: return
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

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    suspend fun addBike(bike: Bike) {
        bikeDao.insert(bike)
        val uid = firebaseAuth.currentUser?.uid ?: return
        remoteDataSource.upsertBike(uid, bike)
    }

    suspend fun replaceBikeId(oldBikeId: String, updatedBike: Bike) {
        require(oldBikeId != updatedBike.id) { "New bike id must differ from the old id" }
        // Upsert the Strava-id bike first so Firestore sync never sees an empty fleet mid-link.
        bikeDao.insert(updatedBike)
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            remoteDataSource.upsertBike(uid, updatedBike)
            remoteDataSource.deleteBike(uid, oldBikeId)
        }
        bikeDao.deleteById(oldBikeId)
    }

    suspend fun addComponent(bikeId: String, component: BikeComponent) {
        val bike = bikeDao.observeById(bikeId).first() ?: return
        val updated = bike.withComponent(component)
        bikeDao.insert(updated)
        val uid = firebaseAuth.currentUser?.uid ?: return
        remoteDataSource.upsertBike(uid, updated)
    }

    suspend fun clearLocalAndStopSync() {
        stopSync()
        bikeDao.deleteAll()
    }
}
