package com.isosic.fleetfixer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.isosic.fleetfixer.core.model.Bike

@Database(
    entities = [Bike::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FleetFixerDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
}
