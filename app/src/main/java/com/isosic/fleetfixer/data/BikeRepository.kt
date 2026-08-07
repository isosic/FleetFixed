package com.isosic.fleetfixer.data

import com.google.firebase.auth.FirebaseAuth
import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BikeRepository(
    private val bikeDao: BikeDao,
    private val remoteDataSource: BikeRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
) {

    private var syncJob: Job? = null

    fun observeBikes(): Flow<List<Bike>> = bikeDao.observeAll()

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

    suspend fun clearLocalAndStopSync() {
        stopSync()
        bikeDao.deleteAll()
    }
}
