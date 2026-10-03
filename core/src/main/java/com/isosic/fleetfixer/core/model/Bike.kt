package com.isosic.fleetfixer.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bikes")
data class Bike(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val distanceMeters: Double = 0.0,
    val purchaseDateEpochMillis: Long? = null,
    val components: List<BikeComponent> = emptyList()
) {
    fun componentFor(type: ComponentType): BikeComponent? =
        components.firstOrNull { it.type == type }

    fun missingComponentTypes(): List<ComponentType> =
        ComponentType.allSlots.filter { type -> components.none { it.type == type } }

    fun withComponent(component: BikeComponent): Bike {
        val updated = components
            .filterNot { it.type == component.type }
            .plus(component)
            .sortedBy { it.type.ordinal }
        return copy(components = updated)
    }
}
