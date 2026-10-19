package com.example.service

import android.graphics.RectF
import com.example.model.DetectedObject
import com.example.model.ObjectCategory
import com.example.model.SafetyStatus
import com.example.model.ScenePerception
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class YoloVisionTracker {

    private data class TrackedHistory(
        val lastBox: RectF,
        val lastTimestamp: Long,
        val lastDistance: Float,
        var smoothedSpeedKmh: Float = 0f
    )

    private val trackHistories = mutableMapOf<String, TrackedHistory>()

    // Standard known physical heights in meters for distance calibration
    private fun getKnownHeightMeters(labelFa: String, category: ObjectCategory): Float {
        return when {
            labelFa.contains("موتور") -> 1.15f
            labelFa.contains("ماشین") || labelFa.contains("تاکسی") -> 1.45f
            labelFa.contains("عابر") || labelFa.contains("انسان") -> 1.70f
            labelFa.contains("میله") -> 0.85f
            labelFa.contains("جدول") || labelFa.contains("پله") -> 0.25f
            labelFa.contains("جوب") -> 0.30f
            labelFa.contains("اسکناس") -> 0.078f
            labelFa.contains("دارو") -> 0.06f
            category == ObjectCategory.HAZARD -> 1.0f
            else -> 0.8f
        }
    }

    /**
     * Estimates optical distance in meters based on bounding box height relative to frame
     * using calibrated optical pinhole camera geometry.
     */
    fun estimateDistance(labelFa: String, category: ObjectCategory, box: RectF): Float {
        val normalizedHeight = max(0.04f, box.height())
        val knownHeight = getKnownHeightMeters(labelFa, category)
        // Focal calibration factor ~ 0.85 for typical smartphone camera vertical FOV
        val estimatedMeters = (knownHeight * 0.85f) / normalizedHeight
        return (estimatedMeters * 10f).roundToInt() / 10f
    }

    /**
     * Determines clock direction (from 9 o'clock to 3 o'clock) based on centerX.
     */
    fun calculateClockPosition(centerX: Float): Int {
        return when {
            centerX < 0.2f -> 9
            centerX < 0.35f -> 10
            centerX < 0.45f -> 11
            centerX < 0.55f -> 12
            centerX < 0.65f -> 1
            centerX < 0.8f -> 2
            else -> 3
        }
    }

    /**
     * Analyzes kinematics: speed, velocity vector, trajectory, and collision risk.
     */
    fun trackKinematics(
        id: String,
        box: RectF,
        distanceMeters: Float,
        currentTimeMs: Long = System.currentTimeMillis()
    ): KinematicResult {
        val history = trackHistories[id]
        if (history == null) {
            trackHistories[id] = TrackedHistory(box, currentTimeMs, distanceMeters)
            return KinematicResult(
                speedKmh = 0f,
                velocityVector = Pair(0f, 0f),
                trajectoryFa = "ایستا / بدون حرکت",
                isApproaching = false,
                isCollisionRisk = false,
                timeToCollision = null
            )
        }

        val dtSeconds = max(0.016f, (currentTimeMs - history.lastTimestamp) / 1000f)
        val oldCenter = Pair(history.lastBox.centerX(), history.lastBox.centerY())
        val newCenter = Pair(box.centerX(), box.centerY())

        val dx = (newCenter.first - oldCenter.first) / dtSeconds
        val dy = (newCenter.second - oldCenter.second) / dtSeconds

        // Distance delta (closing speed)
        val deltaDistance = (history.lastDistance - distanceMeters) / dtSeconds // positive if approaching

        // Ground speed estimation based on distance scale and displacement
        val displacementMagnitude = sqrt(dx * dx + dy * dy)
        val rawSpeedMps = displacementMagnitude * distanceMeters * 1.8f
        val rawSpeedKmh = rawSpeedMps * 3.6f

        val smoothedSpeed = (history.smoothedSpeedKmh * 0.6f) + (rawSpeedKmh * 0.4f)
        trackHistories[id] = TrackedHistory(box, currentTimeMs, distanceMeters, smoothedSpeed)

        val isApproaching = deltaDistance > 0.3f || (dy > 0.08f && box.height() > history.lastBox.height())
        val isMoving = smoothedSpeed > 3.0f

        val trajectoryFa = when {
            !isMoving -> "ایستا / ثابت"
            isApproaching && Math.abs(dx) < 0.15f -> "مستقیم به طرف شما ⬇"
            isApproaching && dx > 0.15f -> "نزدیک‌شونده متمایل به راست ↘"
            isApproaching && dx < -0.15f -> "نزدیک‌شونده متمایل به چپ ↙"
            !isApproaching && dy < -0.08f -> "در حال دور شدن ⬆"
            dx > 0.2f -> "عبور افقی از چپ به راست ➡"
            dx < -0.2f -> "عبور افقی از راست به چپ ⬅"
            else -> "در حال حرکت در زاویه"
        }

        // Collision risk evaluation
        var isCollisionRisk = false
        var ttcSeconds: Float? = null

        if (isApproaching && deltaDistance > 0.3f && distanceMeters < 5.0f) {
            val ttc = distanceMeters / deltaDistance
            if (ttc in 0.05f..4.0f) {
                isCollisionRisk = true
                ttcSeconds = (ttc * 10f).roundToInt() / 10f
            }
        } else if (distanceMeters <= 1.5f) {
            isCollisionRisk = true
            ttcSeconds = 0.5f
        }

        return KinematicResult(
            speedKmh = (smoothedSpeed * 10f).roundToInt() / 10f,
            velocityVector = Pair(dx, dy),
            trajectoryFa = trajectoryFa,
            isApproaching = isApproaching,
            isCollisionRisk = isCollisionRisk,
            timeToCollision = ttcSeconds
        )
    }

    data class KinematicResult(
        val speedKmh: Float,
        val velocityVector: Pair<Float, Float>,
        val trajectoryFa: String,
        val isApproaching: Boolean,
        val isCollisionRisk: Boolean,
        val timeToCollision: Float?
    )

    /**
     * Generates an immediate concise voice message for the blind user based on current perception.
     */
    fun generateAudioSummary(objects: List<DetectedObject>, safety: SafetyStatus): String {
        if (objects.isEmpty()) {
            return "محیط روبرو تا فاصله چند متری کاملاً باز و آزاد است."
        }

        val critical = objects.filter { it.isCollisionRisk }
        if (critical.isNotEmpty()) {
            val topThreat = critical.minByOrNull { it.distanceMeters }!!
            val ttcText = topThreat.timeToCollisionSeconds?.let { " ظرف $it ثانیه" } ?: ""
            return "خطر فوری! ${topThreat.labelFa} با سرعت ${topThreat.speedKmh.toInt()} کیلومتر بر ساعت در فاصله ${topThreat.distanceMeters} متری ساعت ${topThreat.clockPosition}${ttcText} در حال نزدیک شدن است!"
        }

        val nearest = objects.minByOrNull { it.distanceMeters }!!
        val clockDirection = when (nearest.clockPosition) {
            12 -> "مستقیم روبرو"
            1, 2, 3 -> "سمت راست (ساعت ${nearest.clockPosition})"
            9, 10, 11 -> "سمت چپ (ساعت ${nearest.clockPosition})"
            else -> "ساعت ${nearest.clockPosition}"
        }

        return "${nearest.labelFa} در فاصله ${nearest.distanceMeters} متری، $clockDirection. وضعیت: ${nearest.trajectoryNameFa}."
    }
}
