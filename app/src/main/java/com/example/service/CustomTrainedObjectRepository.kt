package com.example.service

import android.graphics.RectF
import com.example.model.CustomTrainedClass
import com.example.model.DetectedObject
import com.example.model.ObjectCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CustomTrainedObjectRepository {

    private val _trainedClasses = MutableStateFlow<List<CustomTrainedClass>>(emptyList())
    val trainedClasses: StateFlow<List<CustomTrainedClass>> = _trainedClasses.asStateFlow()

    fun addTrainedClass(customClass: CustomTrainedClass) {
        val current = _trainedClasses.value.toMutableList()
        current.removeAll { it.nameFa == customClass.nameFa }
        current.add(0, customClass)
        _trainedClasses.value = current
    }

    fun removeTrainedClass(id: String) {
        val current = _trainedClasses.value.toMutableList()
        current.removeAll { it.id == id }
        _trainedClasses.value = current
    }

    /**
     * Converts custom trained classes into detectable objects for the live radar simulation.
     */
    fun createDetectionsFromCustomClasses(): List<DetectedObject> {
        return _trainedClasses.value.mapIndexed { index, custom ->
            val slotOffset = (index * 0.18f) % 0.5f
            DetectedObject(
                id = "custom_${custom.id}",
                labelFa = custom.nameFa,
                labelEn = "Custom: ${custom.nameFa}",
                category = custom.category,
                boundingBox = RectF(0.30f + slotOffset, 0.40f, 0.60f + slotOffset, 0.75f),
                confidence = custom.accuracyScore,
                distanceMeters = 1.3f + (index * 0.6f),
                speedKmh = if (custom.isCollisionThreat) 15f else 0f,
                velocityVector = if (custom.isCollisionThreat) Pair(0.08f, 0.2f) else Pair(0f, 0f),
                trajectoryNameFa = if (custom.isCollisionThreat) "نزدیک‌شونده به سمت شما" else "شیء آموزش‌دیده جدید (ایستا)",
                isApproaching = custom.isCollisionThreat,
                isCollisionRisk = custom.isCollisionThreat,
                timeToCollisionSeconds = if (custom.isCollisionThreat) 2.1f else null,
                clockPosition = 12 + index,
                colorFa = "آموزش‌دیده سفارشی",
                detailedNoteFa = custom.alertAudioTextFa
            )
        }
    }
}
