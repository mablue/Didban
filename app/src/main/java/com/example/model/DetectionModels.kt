package com.example.model

import android.graphics.RectF

enum class ObjectCategory(val titleFa: String) {
    HAZARD("خطر برخورد"),
    VEHICLE("وسیله نقلیه"),
    PEDESTRIAN("عابر پیاده"),
    INFRASTRUCTURE("موانع خیابان و پیاده‌رو"),
    CURRENCY("اسکناس و پول ملی"),
    DAILY_ITEM("اشیاء روزمره")
}

enum class SafetyStatus(val titleFa: String) {
    SAFE("مسیر ایمن و آزاد"),
    CAUTION("احتیاط - مانع در نزدیکی"),
    CRITICAL("خطر حتمی برخورد!")
}

enum class AppMode(val titleFa: String, val subtitleFa: String) {
    RADAR("رادار سرعت و موانع", "محاسبه سرعت، فاصله و جهت حرکت با بیشترین نرخ فریم"),
    IRAN_DATASET("اشیاء بومی ایران", "موتور در پیاده‌رو، جوب، پل فلزی، میله زرد، چاله"),
    CURRENCY_SCAN("اسکناس و دارو", "تشخیص اسکناس‌های ۵۰ و ۱۰۰ تومانی، تراول و داروها"),
    HUMAN_EYE_AI("دید کامل انسانی", "درک عمیق تمام جزئیات صحنه با هوش مصنوعی"),
    TRAIN_MODEL("آموزش مدل", "دانلود از گوگل بدون API، گالری، عکاسی و اعمال افکت")
}

data class DetectedObject(
    val id: String,
    val labelFa: String,
    val labelEn: String,
    val category: ObjectCategory,
    val boundingBox: RectF, // Normalized [0..1]
    val confidence: Float,
    val distanceMeters: Float,
    val speedKmh: Float,
    val velocityVector: Pair<Float, Float>, // dx/dt, dy/dt
    val trajectoryNameFa: String,
    val isApproaching: Boolean,
    val isCollisionRisk: Boolean,
    val timeToCollisionSeconds: Float? = null,
    val clockPosition: Int = 12, // 12 = center, 1..3 = right, 9..11 = left
    val colorFa: String = "مشخص",
    val detailedNoteFa: String = ""
)

data class ScenePerception(
    val objects: List<DetectedObject> = emptyList(),
    val ambientLight: String = "نور مناسب",
    val overallSafety: SafetyStatus = SafetyStatus.SAFE,
    val immediateAudioAlertFa: String = "مسیریاب دیدبان فعال است. محیط آزاد است.",
    val fullAiDescription: String? = null,
    val fps: Int = 30,
    val inferenceLatencyMs: Long = 18L,
    val timestamp: Long = System.currentTimeMillis()
)
