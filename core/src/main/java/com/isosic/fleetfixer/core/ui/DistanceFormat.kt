package com.isosic.fleetfixer.core.ui

import java.util.Locale

fun formatDistanceKm(distanceMeters: Double): String {
    val km = distanceMeters / 1_000.0
    val formatted = if (km < 10.0) {
        String.format(Locale.getDefault(), "%.1f", km)
    } else {
        String.format(Locale.getDefault(), "%.0f", km)
    }
    return "$formatted km"
}
