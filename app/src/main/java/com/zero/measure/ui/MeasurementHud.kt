package com.zero.measure.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zero.measure.calculator.UnitConverter
import com.zero.measure.model.MeasurementPhase
import com.zero.measure.model.MeasurementSessionState
import com.zero.measure.model.MeasurementUnit
import com.zero.measure.model.TrackingStateInfo
import com.zero.measure.viewmodel.MeasurementViewModel
import kotlin.math.roundToInt

@Composable
fun MeasurementHud(
    state: MeasurementSessionState,
    floatingTags: MeasurementViewModel.FloatingTagState,
    onAddPointClicked: () -> Unit,
    onResetClicked: () -> Unit,
    onUnitSelected: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Top Bar: App title and AR Telemetry Badges
        TopStatusBar(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 2. Guidance Instruction Pill
        GuidanceBanner(
            message = state.guidanceMessage,
            isTrackingNormal = state.isTrackingNormal,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 20.dp, end = 20.dp)
        )

        // 3. Projected 3D Anchor Badges (A & B) and Floating Distance Label
        Floating3DMarkers(
            tags = floatingTags,
            modifier = Modifier.fillMaxSize()
        )

        // 4. Bottom Controls: Measurement Card, Unit Selector, and Action Buttons
        BottomControls(
            state = state,
            onAddPointClicked = onAddPointClicked,
            onResetClicked = onResetClicked,
            onUnitSelected = onUnitSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TopStatusBar(
    state: MeasurementSessionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Straighten,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ZERO MEASURE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        // Telemetry Chips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tracking Chip
            val (trackingText, trackingColor) = when (state.trackingStateInfo) {
                TrackingStateInfo.TRACKING -> Pair("AR Active", Color(0xFF22C55E))
                TrackingStateInfo.PAUSED -> Pair("Scanning", Color(0xFFF59E0B))
                TrackingStateInfo.UNSTABLE_MOTION -> Pair("Move Slowly", Color(0xFFEF4444))
                TrackingStateInfo.INSUFFICIENT_FEATURES -> Pair("Low Detail", Color(0xFFF59E0B))
                TrackingStateInfo.INSUFFICIENT_LIGHT -> Pair("Low Light", Color(0xFFF59E0B))
                else -> Pair("Init AR", Color(0xFF94A3B8))
            }
            StatusPill(text = trackingText, dotColor = trackingColor)

            // Planes Chip
            if (state.detectedPlanesCount > 0) {
                StatusPill(text = "${state.detectedPlanesCount} Planes", dotColor = Color(0xFF38BDF8))
            }

            // Depth API Chip
            if (state.depthModeSupported && state.depthModeEnabled) {
                StatusPill(text = "Depth", dotColor = Color(0xFFA855F7))
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, dotColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x331E293B),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GuidanceBanner(
    message: String,
    isTrackingNormal: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isTrackingNormal) Color(0xD90F172A) else Color(0xE67F1D1D),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isTrackingNormal) Color(0x4038BDF8) else Color(0x80EF4444)
        ),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Floating3DMarkers(
    tags: MeasurementViewModel.FloatingTagState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Point A badge
        if (tags.pointAScreenX != null && tags.pointAScreenY != null) {
            PointBadge(
                label = "A",
                color = Color(0xFFFBBF24),
                modifier = Modifier.offset {
                    IntOffset(
                        (tags.pointAScreenX - 16.dp.toPx()).roundToInt(),
                        (tags.pointAScreenY - 42.dp.toPx()).roundToInt()
                    )
                }
            )
        }

        // Point B badge
        if (tags.pointBScreenX != null && tags.pointBScreenY != null) {
            PointBadge(
                label = "B",
                color = Color(0xFFFBBF24),
                modifier = Modifier.offset {
                    IntOffset(
                        (tags.pointBScreenX - 16.dp.toPx()).roundToInt(),
                        (tags.pointBScreenY - 42.dp.toPx()).roundToInt()
                    )
                }
            )
        }

        // Floating line distance tag
        if (tags.midpointScreenX != null && tags.midpointScreenY != null && tags.distanceText != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEE0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                shadowElevation = 8.dp,
                modifier = Modifier.offset {
                    IntOffset(
                        (tags.midpointScreenX - 44.dp.toPx()).roundToInt(),
                        (tags.midpointScreenY - 36.dp.toPx()).roundToInt()
                    )
                }
            ) {
                Text(
                    text = tags.distanceText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PointBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xDD0F172A))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun BottomControls(
    state: MeasurementSessionState,
    onAddPointClicked: () -> Unit,
    onResetClicked: () -> Unit,
    onUnitSelected: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Measurement Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE60F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.phase) {
                    MeasurementPhase.MEASUREMENT_COMPLETE -> {
                        val formatted = state.distanceMeters?.let {
                            UnitConverter.formatDistance(it, state.selectedUnit)
                        } ?: "--"
                        Text(
                            text = "DISTANCE (POINT A → POINT B)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatted,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    MeasurementPhase.POINT_A_PLACED -> {
                        val previewFormatted = state.previewDistanceMeters?.let {
                            UnitConverter.formatDistance(it, state.selectedUnit)
                        } ?: "Move to Point B"
                        Text(
                            text = "POINT A SET ● LIVE ESTIMATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewFormatted,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    MeasurementPhase.SCANNING_SURFACE -> {
                        Text(
                            text = "READY TO MEASURE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.isSurfaceDetected) "Surface Locked" else "Scanning Surface...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isSurfaceDetected) Color(0xFF38BDF8) else Color(0xCCFFFFFF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unit Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    MeasurementUnit.values().forEach { unit ->
                        val isSelected = state.selectedUnit == unit
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFBBF24) else Color(0x331E293B),
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clickable { onUnitSelected(unit) }
                                .testTag("unit_${unit.name.lowercase()}")
                        ) {
                            Text(
                                text = unit.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tactile Action Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            Surface(
                shape = CircleShape,
                color = Color(0xCC1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable { onResetClicked() }
                    .testTag("reset_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Measurement",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Big Center Action Button (+) for Reticle Point Placement
            Surface(
                shape = CircleShape,
                color = if (state.isSurfaceDetected) Color(0xFFFBBF24) else Color(0x66475569),
                shadowElevation = if (state.isSurfaceDetected) 10.dp else 2.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.isSurfaceDetected && state.phase != MeasurementPhase.MEASUREMENT_COMPLETE) {
                        onAddPointClicked()
                    }
                    .testTag("add_point_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Place Point",
                        tint = if (state.isSurfaceDetected) Color(0xFF0F172A) else Color(0x88FFFFFF),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Invisible spacer for visual symmetry opposite to Reset button
            Spacer(modifier = Modifier.size(52.dp))
        }
    }
}
