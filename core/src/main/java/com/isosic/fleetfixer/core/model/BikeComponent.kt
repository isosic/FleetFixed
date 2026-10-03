package com.isosic.fleetfixer.core.model

data class BikeComponent(
    val type: ComponentType,
    val name: String,
    val notes: String = "",
    val dateAddedEpochMillis: Long
)
