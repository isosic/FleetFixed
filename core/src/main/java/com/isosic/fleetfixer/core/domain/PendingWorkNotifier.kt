package com.isosic.fleetfixer.core.domain

interface PendingWorkNotifier {
    fun notifyPendingWorkAdded(bikeId: String)
}
