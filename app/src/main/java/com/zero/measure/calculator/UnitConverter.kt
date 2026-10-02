package com.zero.measure.calculator

import com.zero.measure.model.MeasurementUnit
import java.util.Locale
import kotlin.math.roundToInt

object UnitConverter {
    private const val METERS_TO_CM = 100.0f
    private const val METERS_TO_INCHES = 39.3700787f
    private const val METERS_TO_FEET = 3.2808399f
    private const val INCHES_PER_FOOT = 12

    /**
     * Formats a distance in meters into the target unit string with standard symbols.
     */
    fun formatDistance(meters: Float, unit: MeasurementUnit = MeasurementUnit.AUTO): String {
        if (meters.isNaN() || meters.isInfinite() || meters < 0f) {
            return "--"
        }

        return when (unit) {
            MeasurementUnit.AUTO -> {
                if (meters < 1.0f) {
                    val cm = meters * METERS_TO_CM
                    String.format(Locale.US, "%.1f cm", cm)
                } else {
                    String.format(Locale.US, "%.2f m", meters)
                }
            }
            MeasurementUnit.CENTIMETERS -> {
                val cm = meters * METERS_TO_CM
                String.format(Locale.US, "%.1f cm", cm)
            }
            MeasurementUnit.METERS -> {
                String.format(Locale.US, "%.3f m", meters)
            }
            MeasurementUnit.INCHES -> {
                val inches = meters * METERS_TO_INCHES
                String.format(Locale.US, "%.1f in", inches)
            }
            MeasurementUnit.FEET -> {
                val totalInches = meters * METERS_TO_INCHES
                val feet = (totalInches / INCHES_PER_FOOT).toInt()
                val remInches = totalInches % INCHES_PER_FOOT
                if (feet == 0) {
                    String.format(Locale.US, "%.1f in", remInches)
                } else {
                    String.format(Locale.US, "%d' %.1f\"", feet, remInches)
                }
            }
        }
    }

    /**
     * Converts meters to numeric value in the target unit.
     */
    fun convertValue(meters: Float, unit: MeasurementUnit): Float {
        return when (unit) {
            MeasurementUnit.AUTO, MeasurementUnit.METERS -> meters
            MeasurementUnit.CENTIMETERS -> meters * METERS_TO_CM
            MeasurementUnit.INCHES -> meters * METERS_TO_INCHES
            MeasurementUnit.FEET -> meters * METERS_TO_FEET
        }
    }
}
