package com.dylanpgp.anywhare.service

import androidx.annotation.StringRes
import com.dylanpgp.anywhare.domain.GeoMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

enum class SimMode { POINT, JOYSTICK }

data class LatLng(val lat: Double, val lon: Double)

data class SimState(
    val running: Boolean = false,
    val mode: SimMode = SimMode.POINT,
    val point: LatLng? = null,
    val speedKmh: Float = 5f,
    val bearing: Float = 0f,
    val currentSpeedMs: Float = 0f,
    @StringRes val errorRes: Int? = null,
)

/**
 * Estado compartido (mismo proceso) entre la UI y el servicio.
 * Solo guarda datos primitivos: no retiene Context ni vistas.
 */
object SimulationController {
    const val MIN_SPEED_KMH = 0.1f
    const val MAX_SPEED_KMH = 500f
    private const val DEAD_ZONE = 0.05f

    private val _state = MutableStateFlow(SimState())
    val state: StateFlow<SimState> = _state.asStateFlow()

    @Volatile private var joyX = 0f
    @Volatile private var joyY = 0f

    fun snapshot(): SimState = _state.value

    fun setPoint(p: LatLng) = _state.update { it.copy(point = p) }

    fun setMode(m: SimMode) {
        if (m != SimMode.JOYSTICK) {
            joyX = 0f
            joyY = 0f
        }
        _state.update { it.copy(mode = m, currentSpeedMs = 0f) }
    }

    fun setSpeed(kmh: Float) = _state.update { it.copy(speedKmh = kmh.coerceIn(MIN_SPEED_KMH, MAX_SPEED_KMH)) }

    fun setJoystick(x: Float, y: Float) {
        joyX = x.coerceIn(-1f, 1f)
        joyY = y.coerceIn(-1f, 1f)
    }

    fun setRunning(running: Boolean) = _state.update {
        it.copy(
            running = running,
            errorRes = if (running) null else it.errorRes,
            currentSpeedMs = 0f,
        )
    }

    fun fail(@StringRes res: Int) = _state.update { it.copy(running = false, errorRes = res, currentSpeedMs = 0f) }

    fun clearError() = _state.update { it.copy(errorRes = null) }

    /** Avanza la posición [dtSeconds] según el joystick y la velocidad elegida. */
    fun advance(dtSeconds: Double): SimState {
        val jx = joyX
        val jy = joyY
        return _state.updateAndGet { s ->
            val p = s.point
            val mag = min(1f, hypot(jx, jy))
            if (s.mode != SimMode.JOYSTICK || p == null || mag < DEAD_ZONE) {
                s.copy(currentSpeedMs = 0f)
            } else {
                val v = s.speedKmh / 3.6 * mag
                val bearing = (Math.toDegrees(atan2(jx.toDouble(), jy.toDouble())) + 360.0) % 360.0
                val (lat, lon) = GeoMath.destination(p.lat, p.lon, bearing, v * dtSeconds)
                s.copy(point = LatLng(lat, lon), bearing = bearing.toFloat(), currentSpeedMs = v.toFloat())
            }
        }
    }
}
