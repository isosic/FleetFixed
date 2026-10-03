package com.isosic.fleetfixer.navigation

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val AddBike = "add_bike"
    const val BikeDetail = "bike_detail/{bikeId}"
    const val BikeComponents = "bike_detail/{bikeId}/components"
    const val BikePendingWork = "bike_detail/{bikeId}/pending_work"

    fun bikeDetail(bikeId: String): String = "bike_detail/$bikeId"
    fun bikeComponents(bikeId: String): String = "bike_detail/$bikeId/components"
    fun bikePendingWork(bikeId: String): String = "bike_detail/$bikeId/pending_work"
}
