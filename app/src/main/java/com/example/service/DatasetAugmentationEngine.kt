package com.example.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AugmentedSample(
    val titleFa: String,
    val bitmap: Bitmap
)

class DatasetAugmentationEngine {

    /**
     * Generates diverse computer vision augmentation variations for training on-device.
     */
    suspend fun generateAugmentations(original: Bitmap): List<AugmentedSample> = withContext(Dispatchers.Default) {
        val results = mutableListOf<AugmentedSample>()

        results.add(AugmentedSample("اصلی (بدون افکت)", original))

        // 1. Horizontal Flip
        try {
            val flipMatrix = Matrix().apply { preScale(-1f, 1f) }
            val flipped = Bitmap.createBitmap(original, 0, 0, original.width, original.height, flipMatrix, true)
            results.add(AugmentedSample("انعکاس افقی (زاویه معکوس)", flipped))
        } catch (_: Exception) {}

        // 2. Rotation +12 degrees (Tilt simulation)
        try {
            val rotPosMatrix = Matrix().apply { postRotate(12f) }
            val rotPos = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotPosMatrix, true)
            results.add(AugmentedSample("چرخش زاویه دید (+۱۲°)", rotPos))
        } catch (_: Exception) {}

        // 3. Rotation -12 degrees
        try {
            val rotNegMatrix = Matrix().apply { postRotate(-12f) }
            val rotNeg = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotNegMatrix, true)
            results.add(AugmentedSample("چرخش زاویه دید (-۱۲°)", rotNeg))
        } catch (_: Exception) {}

        // 4. Bright Sunlight Simulation (افزایش روشنایی نور آفتاب ایران)
        try {
            val brightBmp = applyColorMatrix(original) {
                // Brightness boost
                val cm = ColorMatrix(floatArrayOf(
                    1.25f, 0f, 0f, 0f, 40f,
                    0f, 1.25f, 0f, 0f, 40f,
                    0f, 0f, 1.25f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm
            }
            results.add(AugmentedSample("شبیه‌سازی آفتاب شدید روز", brightBmp))
        } catch (_: Exception) {}

        // 5. Night / Low-Light Simulation (دید در شب پیاده‌رو تاریک)
        try {
            val darkBmp = applyColorMatrix(original) {
                val cm = ColorMatrix(floatArrayOf(
                    0.55f, 0f, 0f, 0f, -25f,
                    0f, 0.55f, 0f, 0f, -25f,
                    0f, 0f, 0.55f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm
            }
            results.add(AugmentedSample("شبیه‌سازی تاریکی شب", darkBmp))
        } catch (_: Exception) {}

        // 6. Walking Motion Blur / Hand Tremor Simulation (لرزش دست حین راه رفتن نابینا)
        try {
            val blurBmp = simulateMotionBlur(original)
            results.add(AugmentedSample("لرزش دست و تاری حرکت", blurBmp))
        } catch (_: Exception) {}

        // 7. Structural High Contrast (تقویت کانتور و لبه‌های هندسی شیء)
        try {
            val contrastBmp = applyColorMatrix(original) {
                val cm = ColorMatrix().apply {
                    setSaturation(0.6f)
                }
                cm
            }
            results.add(AugmentedSample("کنتراست لبه‌های ساختاری", contrastBmp))
        } catch (_: Exception) {}

        results
    }

    private fun applyColorMatrix(src: Bitmap, matrixProvider: () -> ColorMatrix): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrixProvider())
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    /**
     * Fast approximation of directional walking motion blur.
     */
    private fun simulateMotionBlur(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            alpha = 140
        }
        canvas.drawBitmap(src, 0f, 0f, null)
        // Offset shift for blur
        canvas.drawBitmap(src, 4f, 2f, paint)
        canvas.drawBitmap(src, -3f, -1f, paint)
        return dest
    }
}
