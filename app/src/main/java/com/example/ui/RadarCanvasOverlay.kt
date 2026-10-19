package com.example.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.model.DetectedObject
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarCanvasOverlay(
    objects: List<DetectedObject>,
    modifier: Modifier = Modifier,
    showRadarSweep: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val bottomY = height * 0.92f // Observer standing at the bottom center

        // 1. Draw Sonar Distance Arcs (1m, 2m, 4m)
        val maxRadius = width * 0.75f
        val ringRadii = listOf(maxRadius * 0.25f, maxRadius * 0.55f, maxRadius * 0.85f)
        val ringLabels = listOf("۱ متر", "۲.۵ متر", "۵ متر")

        ringRadii.forEachIndexed { index, radius ->
            drawCircle(
                color = RadarCyan.copy(alpha = 0.18f),
                radius = radius,
                center = Offset(centerX, bottomY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 2. Draw Sweep Beam if active
        if (showRadarSweep) {
            val rad = Math.toRadians(sweepAngle.toDouble())
            val sweepX = centerX + (maxRadius * cos(rad)).toFloat()
            val sweepY = bottomY - Math.abs(maxRadius * sin(rad)).toFloat()
            drawLine(
                color = AmberPrimary.copy(alpha = 0.35f),
                start = Offset(centerX, bottomY),
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. Draw Detected Objects, Bounding Boxes, Labels & Trajectory Vectors
        objects.forEach { obj ->
            val boxLeft = obj.boundingBox.left * width
            val boxTop = obj.boundingBox.top * height
            val boxRight = obj.boundingBox.right * width
            val boxBottom = obj.boundingBox.bottom * height
            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop

            val boxColor = when {
                obj.isCollisionRisk -> DangerRed
                obj.isApproaching -> AmberPrimary
                obj.distanceMeters <= 1.5f -> AmberPrimary
                else -> RadarCyan
            }

            val strokeWidth = if (obj.isCollisionRisk) 3.5.dp.toPx() else 2.dp.toPx()
            val currentAlpha = if (obj.isCollisionRisk) pulseAlpha else 0.85f

            // Bounding Rect
            drawRect(
                color = boxColor.copy(alpha = currentAlpha),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = strokeWidth)
            )

            // Corner Accents for High-Tech HUD feel
            val cornerLen = minOf(boxWidth, boxHeight) * 0.2f
            drawLine(boxColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), strokeWidth * 1.5f)
            drawLine(boxColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), strokeWidth * 1.5f)
            drawLine(boxColor, Offset(boxRight, boxTop), Offset(boxRight - cornerLen, boxTop), strokeWidth * 1.5f)
            drawLine(boxColor, Offset(boxRight, boxTop), Offset(boxRight, boxTop + cornerLen), strokeWidth * 1.5f)

            // Trajectory Vector Arrow
            val objCenterX = (boxLeft + boxRight) / 2f
            val objCenterY = (boxTop + boxBottom) / 2f
            val vx = obj.velocityVector.first
            val vy = obj.velocityVector.second

            if (obj.speedKmh > 2.0f || obj.isApproaching) {
                val arrowScale = 120f
                val arrowEndX = objCenterX + (vx * arrowScale)
                val arrowEndY = objCenterY + (vy * arrowScale)

                drawLine(
                    color = boxColor,
                    start = Offset(objCenterX, objCenterY),
                    end = Offset(arrowEndX, arrowEndY),
                    strokeWidth = 3.dp.toPx()
                )
                // Arrowhead
                drawCircle(
                    color = boxColor,
                    radius = 5.dp.toPx(),
                    center = Offset(arrowEndX, arrowEndY)
                )
            }

            // Draw Persian Label Badge with Android Native Canvas Paint
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 34f
                isAntiAlias = true
                isFakeBoldText = true
            }

            val bgPaint = Paint().apply {
                color = if (obj.isCollisionRisk) android.graphics.Color.parseColor("#B71C1C")
                else android.graphics.Color.parseColor("#1B2A4A")
                style = Paint.Style.FILL
            }

            val speedText = if (obj.speedKmh > 0f) " | ${obj.speedKmh.toInt()} km/h" else ""
            val infoText = "${obj.labelFa} | ${obj.distanceMeters}m$speedText"
            val textWidth = textPaint.measureText(infoText)
            val padding = 16f
            val badgeHeight = 50f
            val badgeTop = maxOf(20f, boxTop - badgeHeight - 8f)

            drawContext.canvas.nativeCanvas.drawRoundRect(
                boxLeft,
                badgeTop,
                boxLeft + textWidth + (padding * 2),
                badgeTop + badgeHeight,
                12f,
                12f,
                bgPaint
            )

            drawContext.canvas.nativeCanvas.drawText(
                infoText,
                boxLeft + padding,
                badgeTop + badgeHeight - 14f,
                textPaint
            )
        }
    }
}
