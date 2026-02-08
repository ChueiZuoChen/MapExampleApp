package com.example.mapexampleapp.ui.view.mapbox

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.mapexampleapp.R
import com.example.mapexampleapp.service.LocationTrackingService
import com.example.mapexampleapp.state.LocationState
import com.example.mapexampleapp.viewmodel.LocationViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.ThemeValue
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.animation.MapAnimationOptions

@Composable
fun MapboxView(
    viewModel: LocationViewModel,
    onStart: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
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
        onStart(true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            LocationState.Idle -> {
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

            LocationState.RequestingPermission -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Requesting location permission...")
            }

            LocationState.StartingService -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Starting location service...")
            }

            LocationState.StoppingService -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Stopping location service...")
            }

            is LocationState.Tracking -> {
                val trackingState = state as LocationState.Tracking
                trackingState.location?.let { location ->
                    Log.d("LocationService", "LocationScreen: $location")
                    MapboxMap(
                        modifier = Modifier.fillMaxSize(),
                        mapViewportState = rememberMapViewportState {
                            flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .zoom(18.5)
                                    .pitch(45.0)
                                    .bearing(0.0)
                                    .center(Point.fromLngLat(location.longitude, location.latitude))
                                    .build(),
                                animationOptions = MapAnimationOptions.mapAnimationOptions {
                                    duration(300)
                                    Easing { t -> t * t * (3f - 2f * t) }
                                }
                            )
                        },
                        style = {
                            MapboxStandardStyle(
                                standardStyleState = rememberStandardStyleState {
                                    configurationsState.apply {
                                        lightPreset =
                                            if (isSystemInDarkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY
                                        theme = ThemeValue.FADED
                                    }
                                }
                            )
                        }
                    ) {
                        val marker = rememberIconImage(R.drawable.taxi_2401174)
                        PointAnnotation(Point.fromLngLat(location.longitude, location.latitude)) {
                            iconSize = 0.3
                            iconImage = marker
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
    }
}