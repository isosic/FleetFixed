package com.isosic.fleetfixer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bikes")
data class Bike(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String
)
