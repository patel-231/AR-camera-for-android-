package com.zero.measure.ar.anchor

import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import com.zero.measure.model.Point3D

class ArAnchorManager {

    var anchorA: Anchor? = null
        private set

    var anchorB: Anchor? = null
        private set

    /**
     * Creates an ARCore anchor for Point A from a real-world HitResult.
     */
    fun createPointA(hitResult: HitResult): Point3D {
        anchorA?.detach()
        val anchor = hitResult.createAnchor()
        anchorA = anchor
        val pose = anchor.pose
        return Point3D(
            x = pose.tx(),
            y = pose.ty(),
            z = pose.tz(),
            label = "A",
            anchor = anchor
        )
    }

    /**
     * Creates an ARCore anchor for Point B from a real-world HitResult.
     */
    fun createPointB(hitResult: HitResult): Point3D {
        anchorB?.detach()
        val anchor = hitResult.createAnchor()
        anchorB = anchor
        val pose = anchor.pose
        return Point3D(
            x = pose.tx(),
            y = pose.ty(),
            z = pose.tz(),
            label = "B",
            anchor = anchor
        )
    }

    /**
     * Refreshes world coordinates from the ARCore anchors to handle SLAM loop closures
     * and ongoing world tracking updates.
     */
    fun getUpdatedPointA(current: Point3D?): Point3D? {
        val anchor = anchorA ?: return current
        if (anchor.trackingState == TrackingState.TRACKING) {
            val pose = anchor.pose
            return current?.copy(
                x = pose.tx(),
                y = pose.ty(),
                z = pose.tz()
            ) ?: Point3D(pose.tx(), pose.ty(), pose.tz(), "A", anchor)
        }
        return current
    }

    /**
     * Refreshes world coordinates from the ARCore anchors for Point B.
     */
    fun getUpdatedPointB(current: Point3D?): Point3D? {
        val anchor = anchorB ?: return current
        if (anchor.trackingState == TrackingState.TRACKING) {
            val pose = anchor.pose
            return current?.copy(
                x = pose.tx(),
                y = pose.ty(),
                z = pose.tz()
            ) ?: Point3D(pose.tx(), pose.ty(), pose.tz(), "B", anchor)
        }
        return current
    }

    /**
     * Detaches all active anchors from the ARCore session.
     */
    fun clear() {
        try {
            anchorA?.detach()
        } catch (_: Exception) {}
        anchorA = null

        try {
            anchorB?.detach()
        } catch (_: Exception) {}
        anchorB = null
    }
}
