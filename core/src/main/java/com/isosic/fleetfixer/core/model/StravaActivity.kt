package com.isosic.fleetfixer.core.model

data class StravaActivity(
    val id: Long,
    val distanceMeters: Double,
    val startDateEpochSeconds: Long,
    val gearId: String?
)
