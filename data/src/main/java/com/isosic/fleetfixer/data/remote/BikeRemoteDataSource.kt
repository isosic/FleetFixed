package com.isosic.fleetfixer.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem
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
                            distanceMeters = document.optionalDouble(FIELD_DISTANCE_METERS) ?: 0.0,
                            purchaseDateEpochMillis = document.optionalLong(FIELD_PURCHASE_DATE),
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
            // Merge so omitted fields (e.g. purchaseDate when null locally) are not deleted.
            .set(bikeToMap(bike), SetOptions.merge())
            .await()
    }

    suspend fun deleteBike(uid: String, bikeId: String) {
        bikesCollection(uid)
            .document(bikeId)
            .delete()
            .await()
    }

    private fun bikesCollection(uid: String) =
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_BIKES)

    private fun bikeToMap(bike: Bike): Map<String, Any> = buildMap {
        put(FIELD_ID, bike.id)
        put(FIELD_NAME, bike.name)
        put(FIELD_DISTANCE_METERS, bike.distanceMeters)
        bike.purchaseDateEpochMillis?.let { put(FIELD_PURCHASE_DATE, it) }
        put(
            FIELD_COMPONENTS,
            bike.components.map { component ->
                buildMap<String, Any> {
                    put(FIELD_TYPE, component.type.name)
                    put(FIELD_NAME, component.name)
                    put(FIELD_NOTES, component.notes)
                    component.dateAddedEpochMillis?.let { put(FIELD_DATE_ADDED, it) }
                    component.lastServiceEpochMillis?.let { put(FIELD_LAST_SERVICE, it) }
                    put(FIELD_TOTAL_DISTANCE, component.totalDistanceMeters)
                    put(FIELD_DISTANCE_SINCE_SERVICE, component.distanceSinceServiceMeters)
                    put(
                        FIELD_PENDING_WORK,
                        component.pendingWork.map { item ->
                            mapOf(
                                FIELD_ID to item.id,
                                FIELD_DESCRIPTION to item.description
                            )
                        }
                    )
                }
            }
        )
    }

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
            BikeComponent(
                type = type,
                name = name,
                notes = notes,
                dateAddedEpochMillis = map.optionalLong(FIELD_DATE_ADDED),
                lastServiceEpochMillis = map.optionalLong(FIELD_LAST_SERVICE),
                totalDistanceMeters = map.optionalDouble(FIELD_TOTAL_DISTANCE) ?: 0.0,
                distanceSinceServiceMeters = map.optionalDouble(FIELD_DISTANCE_SINCE_SERVICE) ?: 0.0,
                pendingWork = parsePendingWork(map[FIELD_PENDING_WORK])
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePendingWork(raw: Any?): List<PendingWorkItem> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { entry ->
                when (entry) {
                    is Map<*, *> -> {
                        val description = (entry[FIELD_DESCRIPTION] as? String)
                            ?.takeIf { it.isNotBlank() }
                            ?: (entry[FIELD_NAME] as? String)?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        PendingWorkItem(
                            id = (entry[FIELD_ID] as? String)?.takeIf { it.isNotBlank() }
                                ?: java.util.UUID.randomUUID().toString(),
                            description = description
                        )
                    }
                    is String -> entry.takeIf { it.isNotBlank() }?.let {
                        PendingWorkItem(description = it)
                    }
                    else -> null
                }
            }
            is String -> if (raw.isNotBlank()) {
                listOf(PendingWorkItem(description = raw))
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun Map<*, *>.optionalLong(key: String): Long? =
        when (val value = this[key]) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }

    private fun Map<*, *>.optionalDouble(key: String): Double? =
        when (val value = this[key]) {
            is Double -> value
            is Number -> value.toDouble()
            else -> null
        }

    private fun DocumentSnapshot.optionalLong(field: String): Long? =
        when (val value = get(field)) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }

    private fun DocumentSnapshot.optionalDouble(field: String): Double? =
        when (val value = get(field)) {
            is Double -> value
            is Number -> value.toDouble()
            else -> null
        }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BIKES = "bikes"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_DISTANCE_METERS = "distanceMeters"
        const val FIELD_PURCHASE_DATE = "purchaseDate"
        const val FIELD_COMPONENTS = "components"
        const val FIELD_TYPE = "type"
        const val FIELD_NOTES = "notes"
        const val FIELD_DATE_ADDED = "dateAdded"
        const val FIELD_LAST_SERVICE = "lastService"
        const val FIELD_TOTAL_DISTANCE = "totalDistanceMeters"
        const val FIELD_DISTANCE_SINCE_SERVICE = "distanceSinceServiceMeters"
        const val FIELD_PENDING_WORK = "pendingWork"
        const val FIELD_DESCRIPTION = "description"
    }
}
