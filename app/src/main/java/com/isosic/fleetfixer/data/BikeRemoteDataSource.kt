package com.isosic.fleetfixer.data

import com.google.firebase.firestore.FirebaseFirestore
import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BikeRemoteDataSource(
    private val firestore: FirebaseFirestore
) {

    fun observeBikes(uid: String): Flow<List<Bike>> = callbackFlow {
        val registration = bikesCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val bikes = snapshot?.documents
                    ?.mapNotNull { document ->
                        val name = document.getString(FIELD_NAME) ?: return@mapNotNull null
                        Bike(
                            id = document.id,
                            name = name
                        )
                    }
                    .orEmpty()

                trySend(bikes)
            }

        awaitClose { registration.remove() }
    }

    suspend fun upsertBike(uid: String, bike: Bike) {
        bikesCollection(uid)
            .document(bike.id)
            .set(
                mapOf(
                    FIELD_ID to bike.id,
                    FIELD_NAME to bike.name
                )
            )
            .await()
    }

    private fun bikesCollection(uid: String) =
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_BIKES)

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BIKES = "bikes"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
    }
}
