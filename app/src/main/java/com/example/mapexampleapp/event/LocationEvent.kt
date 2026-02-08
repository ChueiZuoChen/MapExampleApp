package com.example.mapexampleapp.event

sealed class LocationEvent {
    data class ShowMessage(val message: String) : LocationEvent()
    data object RequestLocationPermission : LocationEvent()
    data object StartForegroundService : LocationEvent()
    data object StopForegroundService : LocationEvent()
}