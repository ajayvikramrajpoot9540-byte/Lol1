package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.repository.FlashDropRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Android Foreground Service for live GPS tracking during active deliveries.
 * Complies with Android battery, background execution, and location privacy requirements.
 * Runs ONLY during active assigned deliveries and terminates automatically on delivery completion.
 */
class DeliveryLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var activeOrderId: String? = null
    private var activeDeliveryManId: String? = null
    private var isTracking = false

    companion object {
        const val TAG = "DeliveryLocationSvc"
        const val CHANNEL_ID = "flashdrop_delivery_channel"
        const val NOTIFICATION_ID = 4122

        const val ACTION_START = "com.example.service.ACTION_START_TRACKING"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_TRACKING"
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_DELIVERY_MAN_ID = "extra_delivery_man_id"

        // Companion callback or listener hook for real-time repository updates
        var onLocationUpdateListener: ((orderId: String, lat: Double, lng: Double, accuracy: Float) -> Unit)? = null

        fun startTracking(context: Context, orderId: String, deliveryManId: String) {
            val intent = Intent(context, DeliveryLocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_DELIVERY_MAN_ID, deliveryManId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, DeliveryLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
                val deliveryManId = intent.getStringExtra(EXTRA_DELIVERY_MAN_ID) ?: ""
                if (orderId.isNotBlank()) {
                    activeOrderId = orderId
                    activeDeliveryManId = deliveryManId
                    startForegroundService(orderId)
                    startLocationUpdates(orderId)
                }
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService(orderId: String) {
        val notification = buildNotification("Live GPS tracking active for order #$orderId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlashDrop Live Delivery Tracking")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlashDrop Delivery Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when real-time delivery GPS coordinates are being gathered for active customer delivery"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates(orderId: String) {
        if (isTracking) return

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L // 5 seconds interval
            ).apply {
                setMinUpdateIntervalMillis(3000L)
                setMinUpdateDistanceMeters(5f) // 5 meters
                setWaitForAccurateLocation(false)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    handleLocationUpdate(orderId, location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            Log.d(TAG, "Started location updates for order $orderId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
            stopSelf()
        }
    }

    private fun handleLocationUpdate(orderId: String, location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val accuracy = location.accuracy

        Log.d(TAG, "GPS Update: lat=$lat, lng=$lng, acc=$accuracy")
        onLocationUpdateListener?.invoke(orderId, lat, lng, accuracy)
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isTracking = false
        activeOrderId = null
        activeDeliveryManId = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        Log.d(TAG, "DeliveryLocationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
