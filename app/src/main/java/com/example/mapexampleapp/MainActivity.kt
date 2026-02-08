package com.example.mapexampleapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mapexampleapp.event.LocationEvent
import com.example.mapexampleapp.intent.LocationIntent
import com.example.mapexampleapp.ui.theme.MapExampleAppTheme
import com.example.mapexampleapp.ui.view.debug.DebugLocationView
import com.example.mapexampleapp.ui.view.mapbox.MapboxView
import com.example.mapexampleapp.viewmodel.LocationViewModel
import com.example.mapexampleapp.viewmodel.LocationViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LocationViewModel by viewModels {
        LocationViewModelFactory(application)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        viewModel.processIntent(LocationIntent.PermissionResult(granted))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** Debug deleteAllLocations */
//        viewModel.deleteAllLocations()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    event?.let { handleEvent(it) }
                }
            }
        }
        setContent {
            MapExampleAppTheme {
                MapboxView(viewModel = viewModel) { start ->
                    if (start) {
                        startTracking()
                    }
                }
                /** Debug */
//                DebugLocationView(viewModel = viewModel,
//                    onStartTracking = { startTracking() },
//                    onStopTracking = { stopTracking() }
//                )
            }
        }
    }

    private fun startTracking() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun stopTracking() {
        viewModel.processIntent(LocationIntent.StopTracking)
    }

    private fun handleEvent(event: LocationEvent) {
        when (event) {
            is LocationEvent.RequestLocationPermission -> {}

            is LocationEvent.StartForegroundService -> {
                bindAndStartService()
            }

            is LocationEvent.StopForegroundService -> {
                LocationViewModel.stopService(this)
            }

            is LocationEvent.ShowMessage -> {
                println("Message: ${event.message}")
            }
        }
        viewModel.clearEvent()
    }

    private fun bindAndStartService() {
        LocationViewModel.startService(this)
    }
}