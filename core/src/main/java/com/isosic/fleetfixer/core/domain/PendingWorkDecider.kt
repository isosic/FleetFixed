package com.isosic.fleetfixer.core.domain

import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem

/**
 * Decides when components should receive automatically scheduled pending work
 * based on distance / moving-time since last service.
 */
object PendingWorkDecider {

    const val RULE_CHAIN_WEAR = "chain_wear_1500km"
    const val RULE_SHOCK_REBUILD = "shock_rebuild_125h"
    const val RULE_FORK_LOWER_LEG = "fork_lower_leg_50h"
    const val RULE_FORK_REBUILD = "fork_rebuild_125h"

    private const val CHAIN_WEAR_METERS = 1_500_000.0
    private const val HOURS_50_SECONDS = 50L * 60L * 60L
    private const val HOURS_125_SECONDS = 125L * 60L * 60L

    fun apply(bike: Bike): Bike {
        if (bike.components.isEmpty()) return bike
        val updatedComponents = bike.components.map { component ->
            component.withScheduledPendingWork()
        }
        return if (updatedComponents == bike.components) {
            bike
        } else {
            bike.copy(components = updatedComponents)
        }
    }

    fun hasNewScheduledWork(before: Bike, after: Bike): Boolean {
        val beforeIds = before.scheduledRuleIds()
        val afterIds = after.scheduledRuleIds()
        return afterIds.any { it !in beforeIds }
    }

    private fun Bike.scheduledRuleIds(): Set<String> =
        components.flatMap { component ->
            component.pendingWork.mapNotNull { it.ruleId }
        }.toSet()

    private fun BikeComponent.withScheduledPendingWork(): BikeComponent {
        val additions = buildList {
            when (type) {
                ComponentType.CHAIN -> {
                    if (distanceSinceServiceMeters >= CHAIN_WEAR_METERS) {
                        add(
                            scheduled(
                                ruleId = RULE_CHAIN_WEAR,
                                description = "Check chain wear and replace if needed"
                            )
                        )
                    }
                }
                ComponentType.SHOCK -> {
                    if (movingTimeSinceServiceSeconds >= HOURS_125_SECONDS) {
                        add(
                            scheduled(
                                ruleId = RULE_SHOCK_REBUILD,
                                description = "Shock rebuild"
                            )
                        )
                    }
                }
                ComponentType.FORK -> {
                    if (movingTimeSinceServiceSeconds >= HOURS_50_SECONDS) {
                        add(
                            scheduled(
                                ruleId = RULE_FORK_LOWER_LEG,
                                description = "Lower leg service"
                            )
                        )
                    }
                    if (movingTimeSinceServiceSeconds >= HOURS_125_SECONDS) {
                        add(
                            scheduled(
                                ruleId = RULE_FORK_REBUILD,
                                description = "Fork rebuild"
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
        if (additions.isEmpty()) return this
        val existingRuleIds = pendingWork.mapNotNull { it.ruleId }.toSet()
        val newItems = additions.filter { it.ruleId !in existingRuleIds }
        if (newItems.isEmpty()) return this
        return copy(pendingWork = pendingWork + newItems)
    }

    private fun scheduled(ruleId: String, description: String): PendingWorkItem =
        PendingWorkItem(
            id = ruleId,
            description = description,
            ruleId = ruleId
        )
}
