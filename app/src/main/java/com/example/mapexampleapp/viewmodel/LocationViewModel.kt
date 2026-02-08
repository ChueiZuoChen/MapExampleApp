package com.example.mapexampleapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapexampleapp.event.LocationEvent
import com.example.mapexampleapp.intent.LocationIntent
import com.example.mapexampleapp.service.LocationTrackingService
import com.example.mapexampleapp.state.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationViewModel(
    private val app: Application
) : ViewModel() {
    private val _state = MutableStateFlow<LocationState>(LocationState.Idle)
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private val _events = MutableStateFlow<LocationEvent?>(null)
    val events = _events.asStateFlow()

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
                                currentState.copy(
                                    location = intent.location,
                                    error = null
                                )
                            }

                            else -> LocationState.Tracking(
                                location = intent.location,
                                isTracking = true
                            )
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