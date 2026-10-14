package com.dylanpgp.anywhare.domain

sealed interface ParseResult {
    data class Valid(val lat: Double, val lon: Double) : ParseResult
    data object OutOfRange : ParseResult
    data object NotCoordinates : ParseResult
}

/** Interpreta entradas tipo "40.4168, -3.7038" con validación por Regex y rangos. */
object CoordinateParser {
    private val REGEX = Regex("""^\s*([+-]?\d{1,3}(?:\.\d+)?)(?:\s*[,;]\s*|\s+)([+-]?\d{1,3}(?:\.\d+)?)\s*$""")

    fun parse(input: String): ParseResult {
        if (input.length > 64) return ParseResult.NotCoordinates
        val match = REGEX.matchEntire(input) ?: return ParseResult.NotCoordinates
        val lat = match.groupValues[1].toDoubleOrNull()
        val lon = match.groupValues[2].toDoubleOrNull()
        if (lat == null || lon == null) return ParseResult.NotCoordinates
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) {
            ParseResult.Valid(lat, lon)
        } else {
            ParseResult.OutOfRange
        }
    }
}
