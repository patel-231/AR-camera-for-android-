package com.zero.measure.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zero.measure.ar.session.ArSessionManager
import com.zero.measure.model.ArCoreAvailability
import com.zero.measure.model.MeasurementPhase
import com.zero.measure.viewmodel.MeasurementViewModel

@Composable
fun ArMeasurementScreen(
    viewModel: MeasurementViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.uiState.collectAsState()
    val floatingTags by viewModel.floatingTags.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var arSessionInitialized by remember { mutableStateOf(false) }
    var arInitError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        viewModel.onCameraPermissionResult(isGranted)
    }

    // Request camera permission on initial launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.onCameraPermissionResult(true)
        }
    }

    // Initialize ARCore session once permission is granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !arSessionInitialized) {
            when (val result = viewModel.sessionManager.initSession(activity)) {
                is ArSessionManager.SessionResult.Success -> {
                    arSessionInitialized = true
                    arInitError = null
                    viewModel.checkArCoreAvailability()
                }
                is ArSessionManager.SessionResult.InstallRequested -> {
                    // Google Play Services for AR installation requested
                }
                is ArSessionManager.SessionResult.Error -> {
                    arInitError = result.message
                }
            }
        }
    }

    // Manage ARCore session lifecycle
    DisposableEffect(lifecycleOwner, arSessionInitialized) {
        val observer = LifecycleEventObserver { _, event ->
            if (arSessionInitialized) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        viewModel.resumeSession()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        viewModel.pauseSession()
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when {
            // Case 1: Camera permission not granted
            !hasCameraPermission -> {
                CameraPermissionScreen(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            // Case 2: ARCore unavailable on this device
            arInitError != null || state.arCoreAvailability == ArCoreAvailability.UNSUPPORTED -> {
                ArUnsupportedScreen(
                    errorMessage = arInitError,
                    onRetryClicked = {
                        arInitError = null
                        arSessionInitialized = false
                        when (val result = viewModel.sessionManager.initSession(activity)) {
                            is ArSessionManager.SessionResult.Success -> {
                                arSessionInitialized = true
                                viewModel.checkArCoreAvailability()
                            }
                            is ArSessionManager.SessionResult.Error -> {
                                arInitError = result.message
                            }
                            else -> {}
                        }
                    }
                )
            }

            // Case 3: Live AR measurement camera view
            else -> {
                val glSurfaceView = remember {
                    GLSurfaceView(context).apply {
                        preserveEGLContextOnPause = true
                        setEGLContextClientVersion(2)
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                        val r = viewModel.initializeRenderer(activity)
                        setRenderer(r)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                viewModel.onScreenTapped(event.x, event.y)
                            }
                            true
                        }
                    }
                }

                // AR GL Surface View
                AndroidView(
                    factory = { glSurfaceView },
                    modifier = Modifier.fillMaxSize()
                )

                // Precision Center Reticle
                ReticleOverlay(
                    isSurfaceDetected = state.isSurfaceDetected,
                    isPointAPlaced = state.phase == MeasurementPhase.POINT_A_PLACED
                )

                // Measurement HUD
                MeasurementHud(
                    state = state,
                    floatingTags = floatingTags,
                    onAddPointClicked = { viewModel.onPlacePointAtReticle() },
                    onResetClicked = { viewModel.onReset() },
                    onUnitSelected = { unit -> viewModel.onUnitSelected(unit) }
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera Access Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Zero Measure uses the camera and ARCore to track surfaces and measure real-world distances in 3D.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
