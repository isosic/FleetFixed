package com.isosic.fleetfixer.core.model

import java.util.UUID

data class PendingWorkItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String
)
