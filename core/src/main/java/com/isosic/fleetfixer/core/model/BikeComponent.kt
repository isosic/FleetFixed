package com.isosic.fleetfixer.core.model

data class BikeComponent(
    val type: ComponentType,
    val name: String,
    val notes: String = "",
    val dateAddedEpochMillis: Long? = null,
    val lastServiceEpochMillis: Long? = null,
    val totalDistanceMeters: Double = 0.0,
    val distanceSinceServiceMeters: Double = 0.0,
    val pendingWork: List<PendingWorkItem> = emptyList()
)
