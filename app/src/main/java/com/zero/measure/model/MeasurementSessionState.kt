package com.zero.measure.model

enum class MeasurementPhase {
    SCANNING_SURFACE,
    POINT_A_PLACED,
    MEASUREMENT_COMPLETE
}

enum class ArCoreAvailability {
    UNKNOWN,
    CHECKING,
    SUPPORTED,
    UNSUPPORTED,
    INSTALL_REQUESTED
}

enum class TrackingStateInfo {
    NOT_INITIALIZED,
    PAUSED,
    TRACKING,
    STOPPED,
    UNSTABLE_MOTION,
    INSUFFICIENT_FEATURES,
    INSUFFICIENT_LIGHT
}

data class MeasurementSessionState(
    val phase: MeasurementPhase = MeasurementPhase.SCANNING_SURFACE,
    val pointA: Point3D? = null,
    val pointB: Point3D? = null,
    val previewPoint: Point3D? = null,
    val distanceMeters: Float? = null,
    val previewDistanceMeters: Float? = null,
    val selectedUnit: MeasurementUnit = MeasurementUnit.AUTO,
    val isSurfaceDetected: Boolean = false,
    val surfaceTypeDescription: String = "",
    val trackingStateInfo: TrackingStateInfo = TrackingStateInfo.NOT_INITIALIZED,
    val guidanceMessage: String = "Move your phone slowly to scan the surface.",
    val isTrackingNormal: Boolean = false,
    val detectedPlanesCount: Int = 0,
    val depthModeSupported: Boolean = false,
    val depthModeEnabled: Boolean = false,
    val arCoreAvailability: ArCoreAvailability = ArCoreAvailability.CHECKING,
    val cameraPermissionGranted: Boolean = false,
    val errorMessage: String? = null
)
