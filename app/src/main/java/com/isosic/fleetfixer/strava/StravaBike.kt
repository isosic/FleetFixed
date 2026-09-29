package com.isosic.fleetfixer.strava

data class StravaBike(
    val id: String,
    val name: String,
    val primary: Boolean = false,
    val distanceMeters: Double = 0.0
)
