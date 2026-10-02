package com.zero.measure

import com.zero.measure.calculator.DistanceCalculator
import com.zero.measure.calculator.UnitConverter
import com.zero.measure.model.MeasurementUnit
import com.zero.measure.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementCalculationTest {

    @Test
    fun test3DDistanceCalculation_SimpleAxes() {
        val p1 = Point3D(0f, 0f, 0f)
        val p2 = Point3D(3f, 4f, 0f)
        val dist = DistanceCalculator.calculateDistanceMeters(p1, p2)
        assertEquals(5.0f, dist, 0.0001f)
    }

    @Test
    fun test3DDistanceCalculation_AllAxes() {
        // (1, 2, 2) distance is sqrt(1 + 4 + 4) = 3
        val p1 = Point3D(0f, 0f, 0f)
        val p2 = Point3D(1f, 2f, 2f)
        val dist = DistanceCalculator.calculateDistanceMeters(p1, p2)
        assertEquals(3.0f, dist, 0.0001f)
    }

    @Test
    fun testUnitConverter_AutoFormatting() {
        // Less than 1m -> cm
        val shortDist = 0.246f
        val formattedShort = UnitConverter.formatDistance(shortDist, MeasurementUnit.AUTO)
        assertTrue(formattedShort.contains("24.6 cm"))

        // Greater than or equal to 1m -> m
        val longDist = 1.425f
        val formattedLong = UnitConverter.formatDistance(longDist, MeasurementUnit.AUTO)
        assertTrue(formattedLong.contains("1.42 m") || formattedLong.contains("1.43 m"))
    }

    @Test
    fun testUnitConverter_ExplicitUnits() {
        val meters = 1.0f

        val cmStr = UnitConverter.formatDistance(meters, MeasurementUnit.CENTIMETERS)
        assertTrue(cmStr.contains("100.0 cm"))

        val inStr = UnitConverter.formatDistance(meters, MeasurementUnit.INCHES)
        assertTrue(inStr.contains("39.4 in"))

        val ftStr = UnitConverter.formatDistance(meters, MeasurementUnit.FEET)
        assertTrue(ftStr.contains("3' 3.4\"") || ftStr.contains("3'"))
    }
}
