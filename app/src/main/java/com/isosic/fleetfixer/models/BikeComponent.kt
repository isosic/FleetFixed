package com.isosic.fleetfixer.models

data class BikeComponent(
    val type: ComponentType,
    val name: String,
    val notes: String = "",
    val dateAddedEpochMillis: Long
)
