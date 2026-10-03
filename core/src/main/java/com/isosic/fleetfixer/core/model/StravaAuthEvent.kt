package com.isosic.fleetfixer.core.model

sealed interface StravaAuthEvent {
    data object Connected : StravaAuthEvent
    data class Failed(val message: String) : StravaAuthEvent
    data object Cancelled : StravaAuthEvent
}
