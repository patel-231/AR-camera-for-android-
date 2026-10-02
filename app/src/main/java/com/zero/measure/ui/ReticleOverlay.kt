package com.zero.measure.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * iPhone Measure-style high precision center reticle.
 * Expands, highlights, and indicates surface lock when ARCore detects a valid plane.
 */
@Composable
fun ReticleOverlay(
    isSurfaceDetected: Boolean,
    isPointAPlaced: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = when {
            isSurfaceDetected && isPointAPlaced -> Color(0xFFFBBF24) // Gold when measuring
            isSurfaceDetected -> Color(0xFF38BDF8) // Cyan when surface detected
            else -> Color(0x99FFFFFF) // Subtle translucent white when scanning
        },
        animationSpec = tween(durationMillis = 250),
        label = "reticle_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_pulse"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.width / 2f) - 6.dp.toPx()
            val ringRadius = if (isSurfaceDetected) baseRadius * pulseScale else baseRadius * 0.85f

            // Outer target ring
            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = center,
                style = Stroke(width = if (isSurfaceDetected) 2.5.dp.toPx() else 1.5.dp.toPx())
            )

            // Center precision dot
            drawCircle(
                color = if (isSurfaceDetected) ringColor else Color.White,
                radius = if (isSurfaceDetected) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = center
            )

            // Crosshair tick marks when surface is locked
            if (isSurfaceDetected) {
                val tickLength = 5.dp.toPx()
                val gap = ringRadius + 2.dp.toPx()

                // Top tick
                drawLine(
                    color = ringColor,
                    start = Offset(center.x, center.y - gap),
                    end = Offset(center.x, center.y - gap - tickLength),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Bottom tick
                drawLine(
                    color = ringColor,
                    start = Offset(center.x, center.y + gap),
                    end = Offset(center.x, center.y + gap + tickLength),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Left tick
                drawLine(
                    color = ringColor,
                    start = Offset(center.x - gap, center.y),
                    end = Offset(center.x - gap - tickLength, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Right tick
                drawLine(
                    color = ringColor,
                    start = Offset(center.x + gap, center.y),
                    end = Offset(center.x + gap + tickLength, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
