package com.isosic.fleetfixer.navigation

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val AddBike = "add_bike"
    const val BikeDetail = "bike_detail/{bikeId}"
    const val BikeComponents = "bike_detail/{bikeId}/components"
    const val ComponentDetail = "bike_detail/{bikeId}/components/{componentType}"
    const val BikePendingWork = "bike_detail/{bikeId}/pending_work"

    fun bikeDetail(bikeId: String): String =
        "bike_detail/${android.net.Uri.encode(bikeId)}"

    fun bikeComponents(bikeId: String): String =
        "bike_detail/${android.net.Uri.encode(bikeId)}/components"

    fun componentDetail(bikeId: String, componentType: String): String =
        "bike_detail/${android.net.Uri.encode(bikeId)}/components/${android.net.Uri.encode(componentType)}"

    fun bikePendingWork(bikeId: String): String =
        "bike_detail/${android.net.Uri.encode(bikeId)}/pending_work"
}
