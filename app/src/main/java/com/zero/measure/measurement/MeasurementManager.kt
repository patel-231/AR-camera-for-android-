package com.zero.measure.measurement

import com.zero.measure.calculator.DistanceCalculator
import com.zero.measure.calculator.UnitConverter
import com.zero.measure.model.MeasurementPhase
import com.zero.measure.model.MeasurementSessionState
import com.zero.measure.model.MeasurementUnit
import com.zero.measure.model.Point3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MeasurementManager {

    private val _state = MutableStateFlow(MeasurementSessionState())
    val state: StateFlow<MeasurementSessionState> = _state.asStateFlow()

    fun updateUnit(unit: MeasurementUnit) {
        _state.update { it.copy(selectedUnit = unit) }
    }

    fun updateCameraPermission(granted: Boolean) {
        _state.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun updateArCoreDetails(supported: Boolean, depthSupported: Boolean, depthEnabled: Boolean) {
        _state.update {
            it.copy(
                depthModeSupported = depthSupported,
                depthModeEnabled = depthEnabled
            )
        }
    }

    fun onFrameProcessed(
        pointA: Point3D?,
        pointB: Point3D?,
        livePreviewPoint: Point3D?,
        livePreviewDistanceMeters: Float?,
        confirmedDistanceMeters: Float?,
        surfaceDetected: Boolean,
        surfaceType: String,
        detectedPlanesCount: Int,
        guidanceMessage: String,
        trackingStateInfo: com.zero.measure.model.TrackingStateInfo,
        isTrackingNormal: Boolean
    ) {
        val phase = when {
            pointA != null && pointB != null -> MeasurementPhase.MEASUREMENT_COMPLETE
            pointA != null -> MeasurementPhase.POINT_A_PLACED
            else -> MeasurementPhase.SCANNING_SURFACE
        }

        // Calculate confirmed distance if both points present
        val distance = if (pointA != null && pointB != null) {
            confirmedDistanceMeters ?: DistanceCalculator.calculateDistanceMeters(pointA, pointB)
        } else null

        _state.update {
            it.copy(
                phase = phase,
                pointA = pointA,
                pointB = pointB,
                previewPoint = livePreviewPoint,
                distanceMeters = distance,
                previewDistanceMeters = livePreviewDistanceMeters,
                isSurfaceDetected = surfaceDetected,
                surfaceTypeDescription = surfaceType,
                detectedPlanesCount = detectedPlanesCount,
                guidanceMessage = guidanceMessage,
                trackingStateInfo = trackingStateInfo,
                isTrackingNormal = isTrackingNormal
            )
        }
    }

    fun getFormattedDistance(): String {
        val current = _state.value
        val distance = current.distanceMeters ?: return "--"
        return UnitConverter.formatDistance(distance, current.selectedUnit)
    }

    fun getFormattedPreviewDistance(): String {
        val current = _state.value
        val distance = current.previewDistanceMeters ?: return "--"
        return UnitConverter.formatDistance(distance, current.selectedUnit)
    }

    fun reset() {
        _state.update {
            it.copy(
                phase = MeasurementPhase.SCANNING_SURFACE,
                pointA = null,
                pointB = null,
                previewPoint = null,
                distanceMeters = null,
                previewDistanceMeters = null,
                guidanceMessage = "Move your phone slowly to scan the surface."
            )
        }
    }
}
