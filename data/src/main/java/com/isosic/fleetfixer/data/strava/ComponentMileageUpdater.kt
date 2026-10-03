package com.isosic.fleetfixer.data.strava

import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.domain.ComponentMileageCalculator
import com.isosic.fleetfixer.core.domain.ComponentMileageRefresher
import com.isosic.fleetfixer.core.domain.PendingWorkDecider
import com.isosic.fleetfixer.core.domain.PendingWorkNotifier
import com.isosic.fleetfixer.core.domain.StravaBikeRemoteSource
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.StravaActivity
import kotlinx.coroutines.flow.first

class ComponentMileageUpdater(
    private val bikeRepository: BikeRepository,
    private val stravaBikeRemoteSource: StravaBikeRemoteSource,
    private val pendingWorkNotifier: PendingWorkNotifier
) : ComponentMileageRefresher {

    override suspend fun updateAllBikesWithComponents(): Result<Unit> = runCatching {
        val bikes = bikeRepository.observeBikes().first()
        if (bikes.isEmpty()) return@runCatching
        val needsUpdate = bikes.any { bike ->
            bike.components.isNotEmpty() || bike.purchaseDateEpochMillis == null
        }
        if (!needsUpdate) return@runCatching

        val activities = stravaBikeRemoteSource
            .fetchActivities(afterEpochSeconds = afterEpochSecondsFor(bikes))
            .getOrThrow()
        persistUpdatedBikes(bikes, activities)
    }

    override suspend fun updateBike(bikeId: String): Result<Unit> = runCatching {
        val bike = bikeRepository.observeBike(bikeId).first() ?: return@runCatching
        if (bike.components.isEmpty() && bike.purchaseDateEpochMillis != null) return@runCatching

        val activities = stravaBikeRemoteSource
            .fetchActivities(afterEpochSeconds = afterEpochSecondsFor(listOf(bike)))
            .getOrThrow()
        persistUpdatedBikes(listOf(bike), activities)
    }

    private suspend fun persistUpdatedBikes(
        bikes: List<Bike>,
        activities: List<StravaActivity>
    ) {
        bikes.forEach { bike ->
            val withUsage = ComponentMileageCalculator.withStravaDerivedFields(bike, activities)
            val updated = PendingWorkDecider.apply(withUsage)
            if (updated != bike) {
                bikeRepository.addBike(updated)
                if (PendingWorkDecider.hasNewScheduledWork(bike, updated)) {
                    pendingWorkNotifier.notifyPendingWorkAdded(bike.id)
                }
            }
        }
    }

    private fun afterEpochSecondsFor(bikes: List<Bike>): Long? {
        // Need full history to discover first Strava ride when purchase date is missing.
        if (bikes.any { it.purchaseDateEpochMillis == null }) return null
        return earliestRelevantEpochSeconds(bikes)
    }

    private fun earliestRelevantEpochSeconds(bikes: List<Bike>): Long? {
        val millis = bikes.flatMap { bike ->
            buildList {
                bike.purchaseDateEpochMillis?.let(::add)
                bike.components.forEach { component ->
                    component.dateAddedEpochMillis?.let(::add)
                    component.lastServiceEpochMillis?.let(::add)
                }
            }
        }
        return millis.minOrNull()?.div(1_000L)
    }
}
