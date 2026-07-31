package com.isosic.fleetfixer.models

import java.util.UUID

data class Bike(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)