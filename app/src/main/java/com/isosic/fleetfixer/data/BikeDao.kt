package com.isosic.fleetfixer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {

    @Query("SELECT * FROM bikes ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Bike>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bike: Bike)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bikes: List<Bike>)

    @Query("DELETE FROM bikes")
    suspend fun deleteAll()

    @Query("DELETE FROM bikes WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)
}
