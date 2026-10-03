package com.isosic.fleetfixer.core.model

import java.util.UUID

data class PendingWorkItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    /** Stable id for automatically scheduled maintenance rules; null for manual items. */
    val ruleId: String? = null
)

data class CompletedWorkItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val notes: String = "",
    val completedAtEpochMillis: Long,
    val ruleId: String? = null
)
