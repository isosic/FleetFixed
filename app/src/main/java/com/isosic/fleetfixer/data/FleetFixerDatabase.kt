package com.isosic.fleetfixer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.isosic.fleetfixer.models.Bike

@Database(
    entities = [Bike::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FleetFixerDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
}
