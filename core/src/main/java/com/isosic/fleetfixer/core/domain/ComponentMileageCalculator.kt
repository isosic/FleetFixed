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
        return applyUsage(withPurchaseDate, gearActivities)
    }

    fun withUpdatedMileage(
        bike: Bike,
        activities: List<StravaActivity>
    ): Bike {
        if (bike.components.isEmpty()) return bike
        val gearActivities = activities.filter { it.gearId == bike.id }
        return applyUsage(bike, gearActivities)
    }

    private fun applyUsage(
        bike: Bike,
        gearActivities: List<StravaActivity>
    ): Bike {
        if (bike.components.isEmpty()) return bike
        val fallbackMovingTimeSeconds = gearActivities.sumOf { it.movingTimeSeconds }
        val updatedComponents = bike.components.map { component ->
            component.withCalculatedUsage(
                gearActivities = gearActivities,
                fallbackTotalMeters = bike.distanceMeters,
                fallbackMovingTimeSeconds = fallbackMovingTimeSeconds
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

    private fun BikeComponent.withCalculatedUsage(
        gearActivities: List<StravaActivity>,
        fallbackTotalMeters: Double,
        fallbackMovingTimeSeconds: Long
    ): BikeComponent {
        val sinceServiceEpoch = lastServiceEpochMillis ?: dateAddedEpochMillis
        val total = distanceSince(
            activities = gearActivities,
            sinceEpochMillis = dateAddedEpochMillis,
            fallbackMeters = fallbackTotalMeters
        )
        val sinceService = distanceSince(
            activities = gearActivities,
            sinceEpochMillis = sinceServiceEpoch,
            fallbackMeters = fallbackTotalMeters
        )
        val totalMoving = movingTimeSince(
            activities = gearActivities,
            sinceEpochMillis = dateAddedEpochMillis,
            fallbackSeconds = fallbackMovingTimeSeconds
        )
        val movingSinceService = movingTimeSince(
            activities = gearActivities,
            sinceEpochMillis = sinceServiceEpoch,
            fallbackSeconds = fallbackMovingTimeSeconds
        )
        return if (
            totalDistanceMeters == total &&
            distanceSinceServiceMeters == sinceService &&
            totalMovingTimeSeconds == totalMoving &&
            movingTimeSinceServiceSeconds == movingSinceService
        ) {
            this
        } else {
            copy(
                totalDistanceMeters = total,
                distanceSinceServiceMeters = sinceService,
                totalMovingTimeSeconds = totalMoving,
                movingTimeSinceServiceSeconds = movingSinceService
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

    private fun movingTimeSince(
        activities: List<StravaActivity>,
        sinceEpochMillis: Long?,
        fallbackSeconds: Long
    ): Long {
        if (sinceEpochMillis == null) return fallbackSeconds
        val sinceEpochSeconds = sinceEpochMillis / 1_000L
        return activities
            .asSequence()
            .filter { it.startDateEpochSeconds >= sinceEpochSeconds }
            .sumOf { it.movingTimeSeconds }
    }
}
