package com.dylanpgp.anywhare

import com.dylanpgp.anywhare.domain.CoordinateParser
import com.dylanpgp.anywhare.domain.GeoMath
import com.dylanpgp.anywhare.domain.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    @Test fun parsesValidCoordinates() {
        val r = CoordinateParser.parse("40.4168, -3.7038")
        assertTrue(r is ParseResult.Valid)
        r as ParseResult.Valid
        assertEquals(40.4168, r.lat, 1e-9)
        assertEquals(-3.7038, r.lon, 1e-9)
    }

    @Test fun rejectsOutOfRange() {
        assertEquals(ParseResult.OutOfRange, CoordinateParser.parse("91, 10"))
        assertEquals(ParseResult.OutOfRange, CoordinateParser.parse("10, 181"))
    }

    @Test fun ignoresPlainText() {
        assertEquals(ParseResult.NotCoordinates, CoordinateParser.parse("Madrid"))
        assertEquals(ParseResult.NotCoordinates, CoordinateParser.parse("1,2,3"))
    }

    @Test fun movesNorthIncreasesLatitude() {
        val (lat, lon) = GeoMath.destination(0.0, 0.0, 0.0, 1000.0)
        assertTrue(lat > 0.008 && lat < 0.0091)
        assertEquals(0.0, lon, 1e-9)
    }
}
