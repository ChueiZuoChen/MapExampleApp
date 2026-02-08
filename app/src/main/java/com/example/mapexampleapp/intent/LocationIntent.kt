package com.example.mapexampleapp.intent

import android.location.Location

sealed class LocationIntent {
    data object StartTracking : LocationIntent()
    data object StopTracking : LocationIntent()
    data class PermissionResult(val granted: Boolean) : LocationIntent()
    data class LocationUpdate(val location: Location) : LocationIntent()
    data class ServiceStateChanged(val isRunning: Boolean) : LocationIntent()
    data class Error(val message: String) : LocationIntent()
}