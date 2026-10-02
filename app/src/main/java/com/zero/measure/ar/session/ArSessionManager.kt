package com.zero.measure.ar.session

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.zero.measure.model.ArCoreAvailability

class ArSessionManager(private val context: Context) {

    companion object {
        private const val TAG = "ArSessionManager"
    }

    var session: Session? = null
        private set

    var isDepthSupported: Boolean = false
        private set

    var isDepthEnabled: Boolean = false
        private set

    private var installRequested: Boolean = false

    /**
     * Checks whether ARCore is supported on this device (e.g. OPPO Reno8 5G).
     */
    fun checkArCoreAvailability(onResult: (ArCoreAvailability) -> Unit) {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        when {
            availability.isSupported -> {
                onResult(ArCoreAvailability.SUPPORTED)
            }
            availability.isTransient -> {
                onResult(ArCoreAvailability.CHECKING)
            }
            availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                onResult(ArCoreAvailability.UNSUPPORTED)
            }
            else -> {
                onResult(ArCoreAvailability.UNSUPPORTED)
            }
        }
    }

    /**
     * Attempts to create and configure the ARCore session.
     * Handles installation prompt if ARCore is supported but not yet installed.
     */
    fun initSession(activity: Activity): SessionResult {
        var installStatus = ArCoreApk.InstallStatus.INSTALLED
        try {
            installStatus = ArCoreApk.getInstance().requestInstall(activity, !installRequested)
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device is not compatible with ARCore", e)
            return SessionResult.Error("This device is not compatible with ARCore.")
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore is not installed", e)
            return SessionResult.Error("Google Play Services for AR is required.")
        } catch (e: Exception) {
            Log.e(TAG, "ARCore install check failed", e)
            return SessionResult.Error("Failed to verify ARCore installation: ${e.message}")
        }

        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
            installRequested = true
            return SessionResult.InstallRequested
        }

        try {
            val newSession = Session(context)
            val config = Config(newSession).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                focusMode = Config.FocusMode.AUTO
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                // Check Depth API support (OPPO Reno8 5G / ARCore depth support)
                isDepthSupported = newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                if (isDepthSupported) {
                    depthMode = Config.DepthMode.AUTOMATIC
                    isDepthEnabled = true
                    Log.i(TAG, "ARCore Depth API is supported and enabled.")
                } else {
                    depthMode = Config.DepthMode.DISABLED
                    isDepthEnabled = false
                    Log.i(TAG, "ARCore Depth API not supported on this device. Fallback to plane detection.")
                }
            }
            newSession.configure(config)
            session = newSession
            return SessionResult.Success(newSession)
        } catch (e: UnavailableArcoreNotInstalledException) {
            return SessionResult.Error("ARCore is not installed.")
        } catch (e: UnavailableApkTooOldException) {
            return SessionResult.Error("Please update Google Play Services for AR.")
        } catch (e: UnavailableSdkTooOldException) {
            return SessionResult.Error("App SDK version is incompatible with installed ARCore.")
        } catch (e: UnavailableDeviceNotCompatibleException) {
            return SessionResult.Error("This device does not support ARCore.")
        } catch (e: SecurityException) {
            return SessionResult.Error("Camera permission is required to run ARCore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ARCore session", e)
            return SessionResult.Error("ARCore initialization failed: ${e.message}")
        }
    }

    /**
     * Resumes the ARCore session.
     */
    fun resumeSession(): Boolean {
        return try {
            session?.resume()
            true
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera unavailable during session resume", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session resume", e)
            false
        }
    }

    /**
     * Pauses the ARCore session.
     */
    fun pauseSession() {
        try {
            session?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session pause", e)
        }
    }

    /**
     * Destroys and cleans up the ARCore session.
     */
    fun destroySession() {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing session", e)
        } finally {
            session = null
        }
    }

    sealed class SessionResult {
        data class Success(val session: Session) : SessionResult()
        data object InstallRequested : SessionResult()
        data class Error(val message: String) : SessionResult()
    }
}
