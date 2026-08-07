package com.isosic.fleetfixer.data

import com.google.firebase.firestore.FirebaseFirestore
import com.isosic.fleetfixer.models.Bike
import com.isosic.fleetfixer.models.BikeComponent
import com.isosic.fleetfixer.models.ComponentType
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
                            name = name,
                            components = parseComponents(document.get(FIELD_COMPONENTS))
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
            .set(bikeToMap(bike))
            .await()
    }

    private fun bikesCollection(uid: String) =
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_BIKES)

    private fun bikeToMap(bike: Bike): Map<String, Any> =
        mapOf(
            FIELD_ID to bike.id,
            FIELD_NAME to bike.name,
            FIELD_COMPONENTS to bike.components.map { component ->
                mapOf(
                    FIELD_TYPE to component.type.name,
                    FIELD_NAME to component.name,
                    FIELD_NOTES to component.notes,
                    FIELD_DATE_ADDED to component.dateAddedEpochMillis
                )
            }
        )

    @Suppress("UNCHECKED_CAST")
    private fun parseComponents(raw: Any?): List<BikeComponent> {
        val items = raw as? List<*> ?: return emptyList()
        return items.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val typeName = map[FIELD_TYPE] as? String ?: return@mapNotNull null
            val type = runCatching { ComponentType.valueOf(typeName) }.getOrNull()
                ?: return@mapNotNull null
            val name = map[FIELD_NAME] as? String ?: return@mapNotNull null
            val notes = map[FIELD_NOTES] as? String ?: ""
            val dateAdded = when (val value = map[FIELD_DATE_ADDED]) {
                is Long -> value
                is Number -> value.toLong()
                else -> return@mapNotNull null
            }
            BikeComponent(
                type = type,
                name = name,
                notes = notes,
                dateAddedEpochMillis = dateAdded
            )
        }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BIKES = "bikes"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_COMPONENTS = "components"
        const val FIELD_TYPE = "type"
        const val FIELD_NOTES = "notes"
        const val FIELD_DATE_ADDED = "dateAdded"
    }
}
