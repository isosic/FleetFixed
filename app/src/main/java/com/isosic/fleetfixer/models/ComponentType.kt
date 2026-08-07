package com.isosic.fleetfixer.models

enum class ComponentType(val displayName: String) {
    FORK("Fork"),
    HEADSET("Headset"),
    STEM("Stem"),
    HANDLEBAR("Handlebar"),
    GRIPS("Grips"),
    BRAKES("Brakes"),
    ROTORS("Rotors"),
    WHEEL_FRONT("Front wheel"),
    WHEEL_REAR("Rear wheel"),
    TYRE_FRONT("Front tyre"),
    TYRE_REAR("Rear tyre"),
    SHOCK("Shock"),
    SEAT_POST("Seat post"),
    SADDLE("Saddle"),
    SHIFTER("Shifter"),
    DERAILLEUR("Derailleur"),
    CASSETTE("Cassette"),
    CHAIN("Chain");

    companion object {
        val allSlots: List<ComponentType> = entries
    }
}
