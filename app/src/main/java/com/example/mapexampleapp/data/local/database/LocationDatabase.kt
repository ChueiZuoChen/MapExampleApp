package com.example.mapexampleapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mapexampleapp.data.local.dao.LocationDao
import com.example.mapexampleapp.data.local.entity.Converters
import com.example.mapexampleapp.data.local.entity.LocationEntity

@Database(
    entities = [LocationEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null

        fun getDatabase(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}