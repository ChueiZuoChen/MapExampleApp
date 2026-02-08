package com.example.mapexampleapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mapexampleapp.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): Flow<LocationEntity?>
}