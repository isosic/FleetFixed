package com.isosic.fleetfixer.core.domain

import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.StravaActivity

object ComponentMileageCalculator {

    fun withStravaDerivedFields(
        bike: Bike,
        activities: List<StravaActivity>
    ): Bike {
        val gearActivities = activities.filter { it.gearId == bike.id }
        val withPurchaseDate = bike.withPurchaseDateFromFirstActivity(gearActivities)
        return applyMileage(withPurchaseDate, gearActivities)
    }

    fun withUpdatedMileage(
        bike: Bike,
        activities: List<StravaActivity>
    ): Bike {
        if (bike.components.isEmpty()) return bike
        val gearActivities = activities.filter { it.gearId == bike.id }
        return applyMileage(bike, gearActivities)
    }

    private fun applyMileage(
        bike: Bike,
        gearActivities: List<StravaActivity>
    ): Bike {
        if (bike.components.isEmpty()) return bike
        val updatedComponents = bike.components.map { component ->
            component.withCalculatedMileage(
                gearActivities = gearActivities,
                fallbackTotalMeters = bike.distanceMeters
            )
        }
        return if (updatedComponents == bike.components) {
            bike
        } else {
            bike.copy(components = updatedComponents)
        }
    }

    private fun Bike.withPurchaseDateFromFirstActivity(
        gearActivities: List<StravaActivity>
    ): Bike {
        if (purchaseDateEpochMillis != null) return this
        val firstRideEpochSeconds = gearActivities.minOfOrNull { it.startDateEpochSeconds }
            ?: return this
        return copy(purchaseDateEpochMillis = firstRideEpochSeconds * 1_000L)
    }

    private fun BikeComponent.withCalculatedMileage(
        gearActivities: List<StravaActivity>,
        fallbackTotalMeters: Double
    ): BikeComponent {
        val total = distanceSince(
            activities = gearActivities,
            sinceEpochMillis = dateAddedEpochMillis,
            fallbackMeters = fallbackTotalMeters
        )
        val sinceService = distanceSince(
            activities = gearActivities,
            sinceEpochMillis = lastServiceEpochMillis ?: dateAddedEpochMillis,
            fallbackMeters = fallbackTotalMeters
        )
        return if (
            totalDistanceMeters == total &&
            distanceSinceServiceMeters == sinceService
        ) {
            this
        } else {
            copy(
                totalDistanceMeters = total,
                distanceSinceServiceMeters = sinceService
            )
        }
    }

    private fun distanceSince(
        activities: List<StravaActivity>,
        sinceEpochMillis: Long?,
        fallbackMeters: Double
    ): Double {
        if (sinceEpochMillis == null) return fallbackMeters
        val sinceEpochSeconds = sinceEpochMillis / 1_000L
        return activities
            .asSequence()
            .filter { it.startDateEpochSeconds >= sinceEpochSeconds }
            .sumOf { it.distanceMeters }
    }
}
