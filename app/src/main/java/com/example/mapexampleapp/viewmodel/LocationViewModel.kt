package com.example.mapexampleapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapexampleapp.data.local.entity.LocationEntity
import com.example.mapexampleapp.data.repository.LocationRepository
import com.example.mapexampleapp.event.LocationEvent
import com.example.mapexampleapp.intent.LocationIntent
import com.example.mapexampleapp.service.LocationTrackingService
import com.example.mapexampleapp.state.LocationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationViewModel(
    private val app: Application
) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<LocationState>(LocationState.Idle)
    val state: StateFlow<LocationState> = _state.asStateFlow()
    private val _events = MutableStateFlow<LocationEvent?>(null)
    val events = _events.asStateFlow()
    private val locationRepository: LocationRepository by lazy {
        LocationRepository(app.applicationContext)
    }
    val locations: Flow<List<LocationEntity>> = locationRepository.allLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun processIntent(intent: LocationIntent) {
        viewModelScope.launch {
            when (intent) {
                is LocationIntent.StartTracking -> {
                    if (!hasLocationPermission()) {
                        _events.value = LocationEvent.RequestLocationPermission
                    } else {
                        _state.value = LocationState.StartingService
                        _events.value = LocationEvent.StartForegroundService
                    }
                }

                is LocationIntent.StopTracking -> {
                    _state.value = LocationState.StoppingService
                    _events.value = LocationEvent.StopForegroundService
                }

                is LocationIntent.PermissionResult -> {
                    if (intent.granted) {
                        /** trigger start location service progress */
                        _state.value = LocationState.StartingService
                        _events.value = LocationEvent.StartForegroundService
                    } else {
                        _state.update {
                            LocationState.Tracking(
                                error = "Location permission denied"
                            )
                        }
                        _events.value = LocationEvent.ShowMessage("Permission denied")
                    }
                }

                is LocationIntent.LocationUpdate -> {
                    _state.update { currentState ->
                        when (currentState) {
                            is LocationState.Tracking -> {
                                insertLocation(intent.location.latitude, intent.location.longitude)
                                currentState.copy(
                                    location = intent.location,
                                    error = null
                                )

                            }

                            else -> {
                                LocationState.Tracking(
                                    location = intent.location,
                                    isTracking = true
                                )
                            }
                        }
                    }
                }

                is LocationIntent.ServiceStateChanged -> {
                    _state.update {
                        LocationState.Tracking(
                            isTracking = intent.isRunning,
                            error = if (!intent.isRunning) "Service stopped" else null
                        )
                    }
                }

                is LocationIntent.Error -> {
                    _state.update { currentState ->
                        when (currentState) {
                            is LocationState.Tracking -> {
                                currentState.copy(error = intent.message)
                            }

                            else -> LocationState.Tracking(error = intent.message)
                        }
                    }
                    _events.value = LocationEvent.ShowMessage(intent.message)
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return LocationTrackingService.hasLocationPermission(app)
    }

    fun clearEvent() {
        _events.value = null
    }

    private fun insertLocation(lat: Double, long: Double) = viewModelScope.launch {
        try {
            val locationEntity = LocationEntity(0, latitude = lat, longitude = long)
            locationRepository.insertLocation(locationEntity)
        } catch (e: Exception) {
            //error
        }
    }

    fun deleteAllLocations() = viewModelScope.launch {
        locationRepository.deleteAllLocations()
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            intent.action = LocationTrackingService.ACTION_START
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            intent.action = LocationTrackingService.ACTION_STOP
            context.startService(intent)
        }
    }
}