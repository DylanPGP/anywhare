package com.dylanpgp.anywhare.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val id: Long, val name: String, val lat: Double, val lon: Double)

/**
 * Cliente mínimo de Nominatim (OpenStreetMap). Respeta su política de uso:
 * User-Agent propio, máximo ~1 petición/segundo y sin autocompletado.
 */
object NominatimClient {
    private const val BASE = "https://nominatim.openstreetmap.org/search"
    private const val USER_AGENT = "AnyWhare/1.0 (+https://github.com/DylanPGP/anywhare)"
    private const val MAX_BYTES = 512 * 1024
    private const val MIN_INTERVAL_MS = 1_100L
    private const val MAX_RESULTS = 6

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun search(query: String, language: String): List<Place> = mutex.withLock {
        val wait = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastRequestAt)
        if (wait > 0) delay(wait)
        try {
            withContext(Dispatchers.IO) { fetch(query, language) }
        } finally {
            lastRequestAt = SystemClock.elapsedRealtime()
        }
    }

    private fun fetch(query: String, language: String): List<Place> {
        val q = URLEncoder.encode(query, "UTF-8")
        val lang = URLEncoder.encode(language, "UTF-8")
        val url = URL("$BASE?format=jsonv2&limit=$MAX_RESULTS&accept-language=$lang&q=$q")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${conn.responseCode}")
            }
            val body = conn.inputStream.use { readLimited(it) }
            val arr = JSONArray(body)
            val out = ArrayList<Place>(arr.length())
            for (i in 0 until minOf(arr.length(), MAX_RESULTS)) {
                val o = arr.optJSONObject(i) ?: continue
                val lat = o.optString("lat").toDoubleOrNull() ?: continue
                val lon = o.optString("lon").toDoubleOrNull() ?: continue
                val name = o.optString("display_name").trim()
                if (name.isEmpty() || lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
                out += Place(o.optLong("place_id", i.toLong()), name, lat, lon)
            }
            return out
        } finally {
            conn.disconnect()
        }
    }

    private fun readLimited(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw IOException("Respuesta demasiado grande")
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }
}
