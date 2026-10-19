package com.example.service

import com.example.model.DetectedObject
import com.example.model.ObjectCategory
import com.example.model.SafetyStatus

object PersianGuidanceAlgorithm {

    /**
     * Produces instantaneous, sharp, priority-sorted Persian verbal guidance.
     * Evaluates strictly from Critical Hazard -> Proximity Obstacle -> Target Item -> Clear Path.
     * Takes < 1ms to execute, zero network calls.
     */
    fun generatePrioritizedGuidance(
        objects: List<DetectedObject>,
        currentSafety: SafetyStatus
    ): PrioritizedGuidanceResult {
        if (objects.isEmpty()) {
            return PrioritizedGuidanceResult(
                speechText = "مسیر روبرو کاملاً باز و آزاد است.",
                isUrgent = false,
                level = GuidanceLevel.ALL_CLEAR
            )
        }

        // --- LEVEL 1: CRITICAL COLLISION HAZARDS (خطر فوری تصادف) ---
        val collisionThreats = objects.filter { it.isCollisionRisk }
        if (collisionThreats.isNotEmpty()) {
            val topThreat = collisionThreats.minByOrNull { it.distanceMeters }!!
            val directionFa = getClockDirectionLabel(topThreat.clockPosition)
            val speedPart = if (topThreat.speedKmh > 3f) " با سرعت ${topThreat.speedKmh.toInt()} کیلومتر" else ""
            val distPart = " در فاصله ${topThreat.distanceMeters} متری $directionFa"

            val speech = "خطر فوری! ${topThreat.labelFa}$speedPart$distPart به شما نزدیک می‌شود. توقف کنید!"
            return PrioritizedGuidanceResult(
                speechText = speech,
                isUrgent = true,
                level = GuidanceLevel.CRITICAL
            )
        }

        // --- LEVEL 2: IMMINENT TRIP & FALL INFRASTRUCTURE HAZARDS (جوب، چاله، میله زرد) ---
        val closeHazards = objects.filter {
            it.distanceMeters <= 2.2f && (it.category == ObjectCategory.HAZARD || it.category == ObjectCategory.INFRASTRUCTURE)
        }.sortedBy { it.distanceMeters }

        if (closeHazards.isNotEmpty()) {
            val primaryHazard = closeHazards.first()
            val dirFa = getClockDirectionLabel(primaryHazard.clockPosition)

            // Look for pedestrian bridge if gutter is detected
            val bridge = objects.firstOrNull { it.labelFa.contains("پل") }
            val bridgeNote = if (bridge != null) {
                val bDir = getClockDirectionLabel(bridge.clockPosition)
                " پل فلزی در $bDir برای عبور است."
            } else ""

            val speech = "احتیاط: ${primaryHazard.labelFa} در ${primaryHazard.distanceMeters} متری $dirFa.$bridgeNote"
            return PrioritizedGuidanceResult(
                speechText = speech,
                isUrgent = true,
                level = GuidanceLevel.CAUTION
            )
        }

        // --- LEVEL 3: CURRENCY & DAILY LIVING ITEMS (اسکناس و دارو) ---
        val dailyItems = objects.filter {
            it.category == ObjectCategory.CURRENCY || it.category == ObjectCategory.DAILY_ITEM
        }
        if (dailyItems.isNotEmpty()) {
            val item = dailyItems.first()
            val distCm = (item.distanceMeters * 100).toInt()
            val distText = if (distCm < 40) "در دست شما" else "در فاصله $distCm سانتی‌متری"
            val speech = "${item.labelFa} $distText واضح است."
            return PrioritizedGuidanceResult(
                speechText = speech,
                isUrgent = false,
                level = GuidanceLevel.OBJECT_FOUND
            )
        }

        // --- LEVEL 4: OTHER GENERAL OBJECTS / VEHICLES (موانع دورتر یا عابران) ---
        val nearest = objects.minByOrNull { it.distanceMeters }!!
        val dir = getClockDirectionLabel(nearest.clockPosition)
        val movement = if (nearest.speedKmh > 3f) " در حال حرکت" else " ایستا"
        val speech = "${nearest.labelFa} در ${nearest.distanceMeters} متری $dir$movement."

        return PrioritizedGuidanceResult(
            speechText = speech,
            isUrgent = false,
            level = GuidanceLevel.AWARENESS
        )
    }

    private fun getClockDirectionLabel(clock: Int): String {
        return when (clock) {
            12 -> "مستقیم روبرو"
            1 -> "اندکی راست (ساعت ۱)"
            2 -> "سمت راست (ساعت ۲)"
            3 -> "کاملاً سمت راست (ساعت ۳)"
            11 -> "اندکی چپ (ساعت ۱۱)"
            10 -> "سمت چپ (ساعت ۱۰)"
            9 -> "کاملاً سمت چپ (ساعت ۹)"
            else -> "ساعت $clock"
        }
    }

    enum class GuidanceLevel {
        CRITICAL,
        CAUTION,
        OBJECT_FOUND,
        AWARENESS,
        ALL_CLEAR
    }

    data class PrioritizedGuidanceResult(
        val speechText: String,
        val isUrgent: Boolean,
        val level: GuidanceLevel
    )
}
