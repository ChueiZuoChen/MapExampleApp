package com.example.mapexampleapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mapexampleapp.event.LocationEvent
import com.example.mapexampleapp.intent.LocationIntent
import com.example.mapexampleapp.service.LocationTrackingService
import com.example.mapexampleapp.state.LocationState
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    event?.let { handleEvent(it) }
                }
            }
        }

        setContent {
            MaterialTheme {
                LocationScreen(viewModel = viewModel,
                    onStartTracking = { startTracking() },
                    onStopTracking = { stopTracking() })
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
            is LocationEvent.RequestLocationPermission -> {
                // Permission request already handled in startTracking()
            }

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

@Composable
fun LocationScreen(
    viewModel: LocationViewModel, onStartTracking: () -> Unit, onStopTracking: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        val serviceIntent = Intent(context, LocationTrackingService::class.java)
        context.bindService(
            serviceIntent, object : android.content.ServiceConnection {
                override fun onServiceConnected(
                    name: android.content.ComponentName?, service: IBinder?
                ) {
                    val locationService =
                        (service as LocationTrackingService.LocalBinder).getService()
                    locationService.setViewModel(viewModel)
                }

                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    // Service disconnected
                }
            }, Context.BIND_AUTO_CREATE
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is LocationState.Idle -> {
                Text(
                    text = "Location Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Press Start to begin tracking your location",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            is LocationState.RequestingPermission -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Requesting location permission...")
            }

            is LocationState.StartingService -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Starting location service...")
            }

            is LocationState.StoppingService -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Stopping location service...")
            }

            is LocationState.Tracking -> {
                val trackingState = state as LocationState.Tracking

                Text(
                    text = if (trackingState.isTracking) "Location Tracking Active"
                    else "Tracking Stopped",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                trackingState.location?.let { location ->
                    Log.d("LocationService", "LocationScreen: $location")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Latitude: ${location.latitude}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Longitude: ${location.longitude}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Accuracy: ${location.accuracy} meters",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                trackingState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onStartTracking,
                enabled = state !is LocationState.StartingService && state !is LocationState.RequestingPermission && (state as? LocationState.Tracking)?.isTracking != true
            ) {
                Text("Start Tracking")
            }

            Button(
                onClick = onStopTracking,
                enabled = (state as? LocationState.Tracking)?.isTracking == true && state !is LocationState.StoppingService,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Stop Tracking")
            }
        }
    }
}