package com.dylanpgp.anywhare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.dylanpgp.anywhare.MainActivity
import com.dylanpgp.anywhare.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que publica ubicaciones simuladas mediante la API oficial
 * de proveedores de prueba (requiere elegir AnyWhare como app de ubicación simulada
 * en Opciones de desarrollador).
 */
class MockLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var locationManager: LocationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop?.isActive == true) return START_NOT_STICKY

        // startForeground debe llamarse enseguida tras startForegroundService.
        if (!enterForeground()) {
            SimulationController.fail(R.string.error_service)
            stopSelf()
            return START_NOT_STICKY
        }

        val lm = getSystemService(LocationManager::class.java)
        if (lm == null || !setupProviders(lm)) {
            stopSelf()
            return START_NOT_STICKY
        }
        locationManager = lm

        SimulationController.setRunning(true)
        loop = scope.launch { simulate(lm) }
        return START_NOT_STICKY
    }

    private suspend fun simulate(lm: LocationManager) {
        var last = SystemClock.elapsedRealtime()
        push(lm, SimulationController.snapshot())
        while (currentCoroutineContext().isActive) {
            delay(TICK_MS)
            val now = SystemClock.elapsedRealtime()
            val dt = ((now - last) / 1000.0).coerceIn(0.0, 5.0)
            last = now
            push(lm, SimulationController.advance(dt))
        }
    }

    private fun push(lm: LocationManager, s: SimState) {
        val p = s.point ?: return
        for (name in PROVIDERS) {
            try {
                val loc = Location(name).apply {
                    latitude = p.lat
                    longitude = p.lon
                    accuracy = 5f
                    speed = s.currentSpeedMs
                    bearing = s.bearing
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    verticalAccuracyMeters = 5f
                    speedAccuracyMetersPerSecond = 1f
                    bearingAccuracyDegrees = 10f
                }
                lm.setTestProviderLocation(name, loc)
            } catch (e: SecurityException) {
                // El usuario retiró la app de ubicación simulada mientras corría.
                SimulationController.fail(R.string.error_mock)
                stopSelf()
                return
            } catch (e: IllegalArgumentException) {
                // Proveedor eliminado o dato inválido: se ignora este tick.
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupProviders(lm: LocationManager): Boolean {
        return try {
            for (name in PROVIDERS) {
                try { lm.removeTestProvider(name) } catch (e: Exception) { /* no existía */ }
                if (Build.VERSION.SDK_INT >= 31) {
                    lm.addTestProvider(
                        name,
                        ProviderProperties.Builder()
                            .setHasAltitudeSupport(false)
                            .setHasSpeedSupport(true)
                            .setHasBearingSupport(true)
                            .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                            .setAccuracy(ProviderProperties.ACCURACY_FINE)
                            .build(),
                    )
                } else {
                    lm.addTestProvider(name, false, false, false, false, false, true, true, 1, 1)
                }
                lm.setTestProviderEnabled(name, true)
            }
            true
        } catch (e: SecurityException) {
            SimulationController.fail(R.string.error_mock)
            cleanupProviders(lm)
            false
        } catch (e: Exception) {
            SimulationController.fail(R.string.error_service)
            cleanupProviders(lm)
            false
        }
    }

    private fun cleanupProviders(lm: LocationManager) {
        for (name in PROVIDERS) {
            try { lm.setTestProviderEnabled(name, false) } catch (e: Exception) { /* ignorar */ }
            try { lm.removeTestProvider(name) } catch (e: Exception) { /* ignorar */ }
        }
    }

    private fun enterForeground(): Boolean = try {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        true
    } catch (e: Exception) {
        false
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_location)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        locationManager?.let { cleanupProviders(it) }
        locationManager = null
        SimulationController.setRunning(false)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.dylanpgp.anywhare.action.STOP"
        private const val CHANNEL_ID = "simulation"
        private const val NOTIF_ID = 1
        private const val TICK_MS = 1_000L
        private val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}
