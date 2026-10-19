package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private var camera: Camera? = null
    private var isTorchOn = false
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrameAnalyzed: (luminance: Double, bitmap: Bitmap?) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val luminance = calculateLuminance(imageProxy)
                    val bitmap = imageProxy.toBitmapOrNull()
                    onFrameAnalyzed(luminance, bitmap)
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                // Handled gracefully if camera hardware is unavailable
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun calculateLuminance(imageProxy: ImageProxy): Double {
        val plane = imageProxy.planes.firstOrNull() ?: return 128.0
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var total = 0L
        val step = maxOf(1, data.size / 500)
        var count = 0
        for (i in 0 until data.size step step) {
            total += (data[i].toInt() and 0xFF)
            count++
        }
        return if (count > 0) total.toDouble() / count else 128.0
    }

    private fun ImageProxy.toBitmapOrNull(): Bitmap? {
        return try {
            val bitmap = this.toBitmap()
            val rotationDegrees = this.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun toggleTorch(): Boolean {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                isTorchOn = !isTorchOn
                it.cameraControl.enableTorch(isTorchOn)
                return isTorchOn
            }
        }
        return false
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
