package com.example.mapexampleapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mapexampleapp.R
import com.example.mapexampleapp.intent.LocationIntent
import com.example.mapexampleapp.viewmodel.LocationViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1

        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val binder = LocalBinder()
    private var locationManager: LocationManager? = null
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private lateinit var notificationManager: NotificationManager
    private var viewModelRef: WeakReference<LocationViewModel>? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startLocationUpdates()
            }

            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setViewModel(viewModel: LocationViewModel) {
        this.viewModelRef = WeakReference(viewModel)
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking your location in background"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location in background")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission(this)) {
            coroutineScope.launch {
                viewModelRef?.get()?.processIntent(
                    LocationIntent.Error("Location permission not granted")
                )
            }
            return
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled =
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        if (!isGpsEnabled && !isNetworkEnabled) {
            return
        }

        val request = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L
        ).build()


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { latestLocation ->
                    viewModelRef?.get()
                        ?.processIntent(LocationIntent.LocationUpdate(latestLocation))
                }
            }
        }

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(applicationContext)

        try {
            coroutineScope.launch {
                viewModelRef?.get()?.processIntent(
                    LocationIntent.ServiceStateChanged(true)
                )
                locationCallback?.let {
                    fusedLocationProvider?.requestLocationUpdates(
                        request,
                        it,
                        Looper.getMainLooper()
                    )
                }
            }

        } catch (e: SecurityException) {
            Log.e("LocationService", "SecurityException: ${e.message}")
            coroutineScope.launch {
                viewModelRef?.get()?.processIntent(
                    LocationIntent.Error("Location permission error: ${e.message}")
                )
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationProvider?.removeLocationUpdates(it)
        }
        coroutineScope.launch {
            viewModelRef?.get()?.processIntent(
                LocationIntent.ServiceStateChanged(false)
            )
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}