package com.example.mapexampleapp.state

import android.location.Location


sealed class LocationState {
    data object Idle : LocationState()
    data object RequestingPermission : LocationState()
    data object StartingService : LocationState()
    data object StoppingService : LocationState()
    data class Tracking(
        val location: Location? = null,
        val isTracking: Boolean = false,
        val error: String? = null
    ) : LocationState()
}