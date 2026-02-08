package com.example.mapexampleapp.data.repository

import android.content.Context
import com.example.mapexampleapp.data.local.dao.LocationDao
import com.example.mapexampleapp.data.local.database.LocationDatabase
import com.example.mapexampleapp.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

class LocationRepository(
    private val context: Context
) {
    private val locationDatabase: LocationDatabase by lazy {
        LocationDatabase.getDatabase(context)
    }

    private val locationDao: LocationDao by lazy {
        locationDatabase.locationDao()
    }

    val allLocations: Flow<List<LocationEntity>>
        get() = locationDao.getAllLocations()

    suspend fun insertLocation(locationEntity: LocationEntity) {
        locationDao.insertLocation(locationEntity)
    }

    suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }
}