package com.isosic.fleetfixer.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bikes")
data class Bike(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String
) {
    @Ignore
    var components: List<BikeParts> = emptyList()

    @Ignore
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        components: List<BikeParts>
    ) : this(id = id, name = name) {
        this.components = components
    }
}
