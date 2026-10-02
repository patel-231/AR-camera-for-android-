package com.zero.measure.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zero.measure.ar.anchor.ArAnchorManager
import com.zero.measure.ar.hit.ArHitTester
import com.zero.measure.ar.render.ArMeasurementRenderer
import com.zero.measure.ar.session.ArSessionManager
import com.zero.measure.measurement.MeasurementManager
import com.zero.measure.model.ArCoreAvailability
import com.zero.measure.model.MeasurementPhase
import com.zero.measure.model.MeasurementSessionState
import com.zero.measure.model.MeasurementUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = ArSessionManager(application.applicationContext)
    val anchorManager = ArAnchorManager()
    val hitTester = ArHitTester()
    val measurementManager = MeasurementManager()

    val uiState: StateFlow<MeasurementSessionState> = measurementManager.state

    private val _floatingTags = MutableStateFlow<FloatingTagState>(FloatingTagState())
    val floatingTags: StateFlow<FloatingTagState> = _floatingTags.asStateFlow()

    data class FloatingTagState(
        val pointAScreenX: Float? = null,
        val pointAScreenY: Float? = null,
        val pointBScreenX: Float? = null,
        val pointBScreenY: Float? = null,
        val midpointScreenX: Float? = null,
        val midpointScreenY: Float? = null,
        val distanceText: String? = null
    )

    var renderer: ArMeasurementRenderer? = null
        private set

    init {
        checkArCoreAvailability()
    }

    fun checkArCoreAvailability() {
        sessionManager.checkArCoreAvailability { availability ->
            viewModelScope.launch {
                measurementManager.updateArCoreDetails(
                    supported = availability == ArCoreAvailability.SUPPORTED,
                    depthSupported = sessionManager.isDepthSupported,
                    depthEnabled = sessionManager.isDepthEnabled
                )
            }
        }
    }

    fun initializeRenderer(activity: Activity): ArMeasurementRenderer {
        val r = ArMeasurementRenderer(
            context = activity,
            sessionManager = sessionManager,
            anchorManager = anchorManager,
            hitTester = hitTester,
            onFrameProcessed = { frameData ->
                viewModelScope.launch {
                    measurementManager.onFrameProcessed(
                        pointA = frameData.pointA,
                        pointB = frameData.pointB,
                        livePreviewPoint = frameData.livePreviewPoint,
                        livePreviewDistanceMeters = frameData.livePreviewDistanceMeters,
                        confirmedDistanceMeters = frameData.confirmedDistanceMeters,
                        surfaceDetected = frameData.surfaceDetected,
                        surfaceType = frameData.surfaceType,
                        detectedPlanesCount = frameData.detectedPlanesCount,
                        guidanceMessage = frameData.guidanceMessage,
                        trackingStateInfo = frameData.trackingStateInfo,
                        isTrackingNormal = frameData.isTrackingNormal
                    )

                    // Update screen projection tags for badges
                    val distText = if (frameData.pointB != null) {
                        measurementManager.getFormattedDistance()
                    } else if (frameData.livePreviewDistanceMeters != null) {
                        measurementManager.getFormattedPreviewDistance()
                    } else null

                    _floatingTags.update {
                        FloatingTagState(
                            pointAScreenX = frameData.pointA?.screenX,
                            pointAScreenY = frameData.pointA?.screenY,
                            pointBScreenX = frameData.pointB?.screenX,
                            pointBScreenY = frameData.pointB?.screenY,
                            midpointScreenX = frameData.midpointScreenX,
                            midpointScreenY = frameData.midpointScreenY,
                            distanceText = distText
                        )
                    }
                }
            }
        )
        renderer = r
        return r
    }

    fun onScreenTapped(x: Float, y: Float) {
        renderer?.queueTap(x, y)
    }

    fun onPlacePointAtReticle() {
        val r = renderer ?: return
        val cx = r.viewportWidth / 2.0f
        val cy = r.viewportHeight / 2.0f
        r.queueTap(cx, cy)
    }

    fun onReset() {
        renderer?.resetMeasurement()
        measurementManager.reset()
        _floatingTags.update { FloatingTagState() }
    }

    fun onUnitSelected(unit: MeasurementUnit) {
        measurementManager.updateUnit(unit)
    }

    fun onCameraPermissionResult(granted: Boolean) {
        measurementManager.updateCameraPermission(granted)
    }

    fun resumeSession(): Boolean {
        return sessionManager.resumeSession()
    }

    fun pauseSession() {
        sessionManager.pauseSession()
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.destroySession()
    }
}
