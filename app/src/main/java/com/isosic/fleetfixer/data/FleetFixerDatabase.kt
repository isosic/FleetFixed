package com.isosic.fleetfixer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.isosic.fleetfixer.models.Bike

@Database(
    entities = [Bike::class],
    version = 1,
    exportSchema = false
)
abstract class FleetFixerDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
}
