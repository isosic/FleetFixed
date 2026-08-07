package com.isosic.fleetfixer.navigation

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val AddBike = "add_bike"
    const val BikeDetail = "bike/{bikeId}"

    fun bikeDetail(bikeId: String): String = "bike/$bikeId"
}
