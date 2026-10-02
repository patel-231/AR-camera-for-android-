package com.zero.measure.model

import com.google.ar.core.Anchor

/**
 * Represents a point in 3D ARCore world space (measured in meters).
 *
 * @param x World X coordinate in meters
 * @param y World Y coordinate in meters
 * @param z World Z coordinate in meters
 * @param label Marker label (e.g. "A", "B", "Live")
 * @param anchor ARCore Anchor attached to the physical trackable surface
 * @param screenX Projected 2D screen coordinate in pixels (null if behind camera)
 * @param screenY Projected 2D screen coordinate in pixels (null if behind camera)
 * @param isVisibleOnScreen Whether the 3D point projects in front of the camera viewport
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val label: String = "",
    val anchor: Anchor? = null,
    val screenX: Float? = null,
    val screenY: Float? = null,
    val isVisibleOnScreen: Boolean = true
) {
    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)
}
