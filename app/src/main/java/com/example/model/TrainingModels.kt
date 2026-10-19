package com.example.model

import android.graphics.Bitmap

data class CustomTrainedClass(
    val id: String,
    val nameFa: String,
    val category: ObjectCategory,
    val physicalHeightMeters: Float,
    val originalImagesCount: Int,
    val augmentedImagesCount: Int,
    val accuracyScore: Float, // e.g. 0.95f
    val isCollisionThreat: Boolean,
    val alertAudioTextFa: String,
    val previewThumbnail: Bitmap? = null,
    val trainedAtMillis: Long = System.currentTimeMillis()
)

data class TrainingProgress(
    val currentEpoch: Int = 0,
    val totalEpochs: Int = 10,
    val loss: Float = 2.45f,
    val accuracy: Float = 0.45f,
    val currentStageFa: String = "در حال آماده‌سازی دیتاست...",
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

enum class DatasetSourceType(val titleFa: String) {
    GOOGLE_SEARCH("جستجو و دانلود از گوگل بدون API"),
    GALLERY_PICKER("انتخاب از گالری"),
    CAMERA_CAPTURE("عکاسی مستقیم با دوربین")
}
