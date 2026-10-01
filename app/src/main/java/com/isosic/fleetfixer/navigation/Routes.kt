package com.isosic.fleetfixer.navigation

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val AddBike = "add_bike"
    const val BikeDetail = "bike/{bikeId}"
    const val BikeComponents = "bike/{bikeId}/components"
    const val BikePendingWork = "bike/{bikeId}/pending_work"

    fun bikeDetail(bikeId: String): String = "bike/$bikeId"
    fun bikeComponents(bikeId: String): String = "bike/$bikeId/components"
    fun bikePendingWork(bikeId: String): String = "bike/$bikeId/pending_work"
}
