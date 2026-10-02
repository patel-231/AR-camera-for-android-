package com.zero.measure.ar.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.zero.measure.ar.anchor.ArAnchorManager
import com.zero.measure.ar.hit.ArHitTester
import com.zero.measure.ar.session.ArSessionManager
import com.zero.measure.model.Point3D
import com.zero.measure.model.TrackingStateInfo
import com.zero.measure.util.HapticHelper
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArMeasurementRenderer(
    private val context: Context,
    private val sessionManager: ArSessionManager,
    private val anchorManager: ArAnchorManager,
    private val hitTester: ArHitTester,
    private val onFrameProcessed: (FrameData) -> Unit
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArMeasurementRenderer"
    }

    data class TapRequest(val screenX: Float, val screenY: Float)

    data class FrameData(
        val trackingStateInfo: TrackingStateInfo,
        val isTrackingNormal: Boolean,
        val surfaceDetected: Boolean,
        val surfaceType: String,
        val detectedPlanesCount: Int,
        val guidanceMessage: String,
        val pointA: Point3D?,
        val pointB: Point3D?,
        val livePreviewPoint: Point3D?,
        val livePreviewDistanceMeters: Float?,
        val confirmedDistanceMeters: Float?,
        val midpointScreenX: Float?,
        val midpointScreenY: Float?
    )

    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    private val planeRenderer = PlaneRenderer()
    private val lineRenderer = WorldLineRenderer()
    private val pointMarkerRenderer = PointMarkerRenderer()

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)

    var viewportWidth: Int = 1
        private set
    var viewportHeight: Int = 1
        private set

    private val tapQueue = ConcurrentLinkedQueue<TapRequest>()

    private var currentPointA: Point3D? = null
    private var currentPointB: Point3D? = null
    private var lastSurfaceDetected: Boolean = false

    fun queueTap(screenX: Float, screenY: Float) {
        tapQueue.offer(TapRequest(screenX, screenY))
    }

    fun resetMeasurement() {
        currentPointA = null
        currentPointB = null
        anchorManager.clear()
        HapticHelper.reset(context)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.06f, 0.09f, 0.16f, 1.0f)

        try {
            backgroundRenderer.createOnGlThread()
            pointCloudRenderer.createOnGlThread()
            planeRenderer.createOnGlThread()
            lineRenderer.createOnGlThread()
            pointMarkerRenderer.createOnGlThread()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OpenGL ES renderers", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display: Display? = windowManager?.defaultDisplay
        val rotation = display?.rotation ?: 0
        sessionManager.session?.setDisplayGeometry(rotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session: Session = sessionManager.session ?: return

        // Update camera texture
        if (backgroundRenderer.textureId != -1) {
            session.setCameraTextureName(backgroundRenderer.textureId)
        }

        val frame: Frame = try {
            session.update()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session.update()", e)
            return
        }

        val camera = frame.camera

        // 1. Draw camera background
        try {
            backgroundRenderer.draw(frame)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing camera background", e)
        }

        // Tracking state evaluation
        val trackingState = camera.trackingState
        val failureReason = camera.trackingFailureReason
        val isTracking = trackingState == TrackingState.TRACKING

        var trackingInfo = TrackingStateInfo.TRACKING
        var guidance = "Move your phone slowly to scan the surface."

        when (trackingState) {
            TrackingState.PAUSED -> {
                when (failureReason) {
                    TrackingFailureReason.EXCESSIVE_MOTION -> {
                        trackingInfo = TrackingStateInfo.UNSTABLE_MOTION
                        guidance = "Moving too fast. Move your phone slowly."
                    }
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                        trackingInfo = TrackingStateInfo.INSUFFICIENT_FEATURES
                        guidance = "Point at a textured surface with good details."
                    }
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                        trackingInfo = TrackingStateInfo.INSUFFICIENT_LIGHT
                        guidance = "Too dark. Point toward better lighting."
                    }
                    else -> {
                        trackingInfo = TrackingStateInfo.PAUSED
                        guidance = "Searching for surfaces. Move slowly."
                    }
                }
            }
            TrackingState.STOPPED -> {
                trackingInfo = TrackingStateInfo.STOPPED
                guidance = "ARCore tracking stopped."
            }
            TrackingState.TRACKING -> {
                trackingInfo = TrackingStateInfo.TRACKING
            }
        }

        val allPlanes = session.getAllTrackables(Plane::class.java)
        var detectedPlanesCount = 0
        for (p in allPlanes) {
            if (p.trackingState == TrackingState.TRACKING && p.subsumedBy == null) {
                detectedPlanesCount++
            }
        }

        // If not tracking, notify and return early
        if (!isTracking) {
            onFrameProcessed(
                FrameData(
                    trackingStateInfo = trackingInfo,
                    isTrackingNormal = false,
                    surfaceDetected = false,
                    surfaceType = "",
                    detectedPlanesCount = detectedPlanesCount,
                    guidanceMessage = guidance,
                    pointA = currentPointA,
                    pointB = currentPointB,
                    livePreviewPoint = null,
                    livePreviewDistanceMeters = null,
                    confirmedDistanceMeters = null,
                    midpointScreenX = null,
                    midpointScreenY = null
                )
            )
            return
        }

        // Camera matrices
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f)
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 2. Draw feature point cloud (visual feedback that SLAM is active)
        try {
            frame.acquirePointCloud().use { pointCloud ->
                pointCloudRenderer.draw(pointCloud, viewMatrix, projMatrix)
            }
        } catch (_: Exception) {}

        // 3. Draw detected planes (subtle visual polygon overlay)
        try {
            planeRenderer.draw(allPlanes, viewMatrix, projMatrix)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing planes", e)
        }

        // 4. Continuous hit test at screen center (crosshair / reticle)
        val centerX = viewportWidth / 2.0f
        val centerY = viewportHeight / 2.0f
        val centerHit = hitTester.performHitTest(frame, centerX, centerY)

        val isSurfaceCurrentlyDetected = centerHit != null
        if (isSurfaceCurrentlyDetected && !lastSurfaceDetected) {
            HapticHelper.surfaceLock(context)
        }
        lastSurfaceDetected = isSurfaceCurrentlyDetected

        // 5. Process user tap requests (screen tap or HUD button)
        while (!tapQueue.isEmpty()) {
            val tap = tapQueue.poll() ?: break
            val hit = hitTester.performHitTest(frame, tap.screenX, tap.screenY)
            if (hit != null) {
                if (currentPointA == null) {
                    // Set Point A
                    val pointA = anchorManager.createPointA(hit.hitResult)
                    currentPointA = pointA
                    HapticHelper.pointPlaced(context)
                } else if (currentPointB == null) {
                    // Set Point B
                    val pointB = anchorManager.createPointB(hit.hitResult)
                    currentPointB = pointB
                    HapticHelper.measurementComplete(context)
                }
            }
        }

        // 6. Update anchor coordinates from ongoing ARCore tracking
        currentPointA = anchorManager.getUpdatedPointA(currentPointA)
        currentPointB = anchorManager.getUpdatedPointB(currentPointB)

        // Project Point A to screen
        val projectedPointA = currentPointA?.let { p ->
            projectWorldToScreen(p.x, p.y, p.z)
        }
        val updatedA = currentPointA?.copy(
            screenX = projectedPointA?.first,
            screenY = projectedPointA?.second,
            isVisibleOnScreen = projectedPointA != null
        )

        // Project Point B to screen
        val projectedPointB = currentPointB?.let { p ->
            projectWorldToScreen(p.x, p.y, p.z)
        }
        val updatedB = currentPointB?.copy(
            screenX = projectedPointB?.first,
            screenY = projectedPointB?.second,
            isVisibleOnScreen = projectedPointB != null
        )

        var livePreviewPoint: Point3D? = null
        var livePreviewDistance: Float? = null
        var confirmedDistance: Float? = null
        var midpointScreenX: Float? = null
        var midpointScreenY: Float? = null

        // 7. Draw 3D measurement geometry in AR world space
        if (updatedA != null && updatedB != null) {
            // Stage: Completed measurement
            guidance = "Measurement complete. Tap Reset to start new measurement."
            pointMarkerRenderer.drawPointMarker(updatedA, viewMatrix, projMatrix)
            pointMarkerRenderer.drawPointMarker(updatedB, viewMatrix, projMatrix)
            lineRenderer.drawLine(updatedA, updatedB, isPreview = false, viewMatrix, projMatrix)

            val dx = updatedB.x - updatedA.x
            val dy = updatedB.y - updatedA.y
            val dz = updatedB.z - updatedA.z
            confirmedDistance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)

            val mx = (updatedA.x + updatedB.x) * 0.5f
            val my = (updatedA.y + updatedB.y) * 0.5f
            val mz = (updatedA.z + updatedB.z) * 0.5f
            val midScreen = projectWorldToScreen(mx, my, mz)
            midpointScreenX = midScreen?.first
            midpointScreenY = midScreen?.second

        } else if (updatedA != null) {
            // Stage: Point A placed, waiting for Point B
            pointMarkerRenderer.drawPointMarker(updatedA, viewMatrix, projMatrix)

            if (centerHit != null) {
                guidance = "Move to the second point and tap."
                val target = centerHit.worldPosition
                livePreviewPoint = target

                lineRenderer.drawLine(updatedA, target, isPreview = true, viewMatrix, projMatrix)

                pointMarkerRenderer.drawPointMarker(
                    target,
                    viewMatrix,
                    projMatrix,
                    ringColor = floatArrayOf(0.22f, 0.74f, 0.97f, 0.9f),
                    centerColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.8f)
                )

                val dx = target.x - updatedA.x
                val dy = target.y - updatedA.y
                val dz = target.z - updatedA.z
                livePreviewDistance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)

                val mx = (updatedA.x + target.x) * 0.5f
                val my = (updatedA.y + target.y) * 0.5f
                val mz = (updatedA.z + target.z) * 0.5f
                val midScreen = projectWorldToScreen(mx, my, mz)
                midpointScreenX = midScreen?.first
                midpointScreenY = midScreen?.second
            } else {
                guidance = "Point at a surface to position Point B."
            }
        } else {
            // Stage: Scanning surface for Point A
            if (centerHit != null) {
                guidance = "Surface detected (${centerHit.surfaceType}). Tap to place Point A."
            } else {
                guidance = if (detectedPlanesCount > 0) {
                    "Aim reticle at a detected plane surface."
                } else {
                    "Move your phone slowly to scan the surface."
                }
            }
        }

        // Pass frame data to UI
        onFrameProcessed(
            FrameData(
                trackingStateInfo = trackingInfo,
                isTrackingNormal = true,
                surfaceDetected = centerHit != null,
                surfaceType = centerHit?.surfaceType ?: "",
                detectedPlanesCount = detectedPlanesCount,
                guidanceMessage = guidance,
                pointA = updatedA,
                pointB = updatedB,
                livePreviewPoint = livePreviewPoint,
                livePreviewDistanceMeters = livePreviewDistance,
                confirmedDistanceMeters = confirmedDistance,
                midpointScreenX = midpointScreenX,
                midpointScreenY = midpointScreenY
            )
        )
    }

    private fun projectWorldToScreen(x: Float, y: Float, z: Float): Pair<Float, Float>? {
        val clip = FloatArray(4)
        val world = floatArrayOf(x, y, z, 1.0f)
        Matrix.multiplyMV(clip, 0, viewProjMatrix, 0, world, 0)

        val w = clip[3]
        if (w <= 0.05f) {
            return null
        }

        val ndcX = clip[0] / w
        val ndcY = clip[1] / w

        val screenX = (ndcX + 1.0f) * 0.5f * viewportWidth
        val screenY = (1.0f - ndcY) * 0.5f * viewportHeight

        return Pair(screenX, screenY)
    }
}
