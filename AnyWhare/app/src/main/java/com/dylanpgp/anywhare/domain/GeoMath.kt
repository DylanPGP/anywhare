package com.dylanpgp.anywhare.domain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** Punto de destino tras recorrer [distanceM] metros con rumbo [bearingDeg]. */
    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceM: Double): Pair<Double, Double> {
        val br = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(lat)
        val lon1 = Math.toRadians(lon)
        val dr = distanceM / EARTH_RADIUS_M
        val lat2 = asin(sin(lat1) * cos(dr) + cos(lat1) * sin(dr) * cos(br))
        val lon2 = lon1 + atan2(sin(br) * sin(dr) * cos(lat1), cos(dr) - sin(lat1) * sin(lat2))
        val lonDeg = ((Math.toDegrees(lon2) + 540.0) % 360.0) - 180.0
        return Math.toDegrees(lat2).coerceIn(-90.0, 90.0) to lonDeg
    }
}
