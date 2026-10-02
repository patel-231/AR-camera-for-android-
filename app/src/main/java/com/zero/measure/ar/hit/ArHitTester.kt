package com.zero.measure.ar.hit

import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.zero.measure.model.Point3D

class ArHitTester {

    data class SurfaceHit(
        val hitResult: HitResult,
        val hitPose: Pose,
        val worldPosition: Point3D,
        val surfaceType: String,
        val distanceToCamera: Float
    )

    /**
     * Performs a real ARCore hit test against detected planes and depth points.
     *
     * @param frame Current ARCore frame
     * @param screenX Pixel X on viewport (e.g. center or touch point)
     * @param screenY Pixel Y on viewport (e.g. center or touch point)
     * @return Closest valid SurfaceHit, or null if no surface was hit
     */
    fun performHitTest(frame: Frame, screenX: Float, screenY: Float): SurfaceHit? {
        val hitResults = frame.hitTest(screenX, screenY)

        var closestHit: SurfaceHit? = null
        var minDistance = Float.MAX_VALUE

        for (hit in hitResults) {
            val trackable = hit.trackable
            val isPlane = trackable is Plane && trackable.trackingState == TrackingState.TRACKING
            val isDepth = trackable is DepthPoint && trackable.trackingState == TrackingState.TRACKING

            if (!isPlane && !isDepth) continue

            // For planes, verify pose is within polygon bounds or valid extent
            var surfaceName = "Surface"
            if (trackable is Plane) {
                if (!trackable.isPoseInPolygon(hit.hitPose) && !trackable.isPoseInExtents(hit.hitPose)) {
                    continue
                }
                surfaceName = when (trackable.type) {
                    Plane.Type.HORIZONTAL_UPWARD_FACING -> "Floor / Table"
                    Plane.Type.HORIZONTAL_DOWNWARD_FACING -> "Ceiling"
                    Plane.Type.VERTICAL -> "Wall"
                    else -> "Surface"
                }
            } else if (trackable is DepthPoint) {
                surfaceName = "Depth Surface"
            }

            val distance = hit.distance
            if (distance in 0.05f..minDistance) {
                minDistance = distance
                val pose = hit.hitPose
                val point = Point3D(
                    x = pose.tx(),
                    y = pose.ty(),
                    z = pose.tz(),
                    label = ""
                )
                closestHit = SurfaceHit(
                    hitResult = hit,
                    hitPose = pose,
                    worldPosition = point,
                    surfaceType = surfaceName,
                    distanceToCamera = distance
                )
            }
        }

        return closestHit
    }
}
