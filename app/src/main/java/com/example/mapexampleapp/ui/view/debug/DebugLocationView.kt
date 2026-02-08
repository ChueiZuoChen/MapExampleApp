package com.example.mapexampleapp.ui.view.debug

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.background
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
import com.example.mapexampleapp.service.LocationTrackingService
import com.example.mapexampleapp.state.LocationState
import com.example.mapexampleapp.viewmodel.LocationViewModel

@Composable
fun DebugLocationView(
    viewModel: LocationViewModel, onStartTracking: () -> Unit, onStopTracking: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val locations by viewModel.locations.collectAsState(emptyList())
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

                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }, Context.BIND_AUTO_CREATE
        )
    }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is LocationState.Idle -> {
                Text(
                    text = "Location Tracker",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Press Start to begin tracking your location",
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
                    color = MaterialTheme.colorScheme.primary,
                    text = if (trackingState.isTracking) "Location Tracking Active"
                    else "Tracking Stopped",
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
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Latitude: ${location.latitude}",
                            )
                            Text(
                                text = "Longitude: ${location.longitude}",
                            )
                            Text(
                                text = "Accuracy: ${location.accuracy} meters",
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
        Spacer(modifier = Modifier.height(32.dp))
        LocationHistoryList(locations)
    }
}