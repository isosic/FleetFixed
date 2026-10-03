package com.isosic.fleetfixer.core.model

data class BikeComponent(
    val type: ComponentType,
    val name: String,
    val notes: String = "",
    val dateAddedEpochMillis: Long? = null,
    val lastServiceEpochMillis: Long? = null,
    val totalDistanceMeters: Double = 0.0,
    val distanceSinceServiceMeters: Double = 0.0,
    val totalMovingTimeSeconds: Long = 0L,
    val movingTimeSinceServiceSeconds: Long = 0L,
    val pendingWork: List<PendingWorkItem> = emptyList(),
    val completedWork: List<CompletedWorkItem> = emptyList()
) {
    fun completePendingWork(
        itemId: String,
        completedAtEpochMillis: Long = System.currentTimeMillis()
    ): BikeComponent? {
        val item = pendingWork.find { it.id == itemId } ?: return null
        return copy(
            pendingWork = pendingWork.filterNot { it.id == itemId },
            completedWork = listOf(
                CompletedWorkItem(
                    id = item.id,
                    description = item.description,
                    notes = "",
                    completedAtEpochMillis = completedAtEpochMillis,
                    ruleId = item.ruleId
                )
            ) + completedWork,
            lastServiceEpochMillis = completedAtEpochMillis,
            distanceSinceServiceMeters = 0.0,
            movingTimeSinceServiceSeconds = 0L
        )
    }
}
