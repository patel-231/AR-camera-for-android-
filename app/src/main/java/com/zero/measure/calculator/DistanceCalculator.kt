package com.zero.measure.calculator

import com.zero.measure.model.Point3D
import kotlin.math.sqrt

object DistanceCalculator {

    /**
     * Calculates the real-world Euclidean distance in 3D ARCore world space (meters).
     *
     * distance = sqrt((x2 - x1)^2 + (y2 - y1)^2 + (z2 - z1)^2)
     */
    fun calculateDistanceMeters(p1: Point3D, p2: Point3D): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates the midpoint between two points in 3D AR space.
     */
    fun calculateMidpoint(p1: Point3D, p2: Point3D): FloatArray {
        return floatArrayOf(
            (p1.x + p2.x) * 0.5f,
            (p1.y + p2.y) * 0.5f,
            (p1.z + p2.z) * 0.5f
        )
    }
}
