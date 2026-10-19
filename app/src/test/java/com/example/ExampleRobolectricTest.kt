package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.model.CustomTrainedClass
import com.example.model.ObjectCategory
import com.example.model.SafetyStatus
import com.example.service.CustomTrainedObjectRepository
import com.example.service.DatasetAugmentationEngine
import com.example.service.IranDatasetRepository
import com.example.service.OnDeviceTrainer
import com.example.service.YoloVisionTracker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private val tracker = YoloVisionTracker()
    private val augmentationEngine = DatasetAugmentationEngine()
    private val trainer = OnDeviceTrainer()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("دیدبان", appName)
    }

    @Test
    fun `test clock position calculation`() {
        assertEquals(9, tracker.calculateClockPosition(0.1f))
        assertEquals(10, tracker.calculateClockPosition(0.3f))
        assertEquals(12, tracker.calculateClockPosition(0.5f))
        assertEquals(2, tracker.calculateClockPosition(0.75f))
        assertEquals(3, tracker.calculateClockPosition(0.9f))
    }

    @Test
    fun `test distance estimation`() {
        val motorBox = RectF(0.2f, 0.2f, 0.6f, 0.7f) // height = 0.5f
        val distance = tracker.estimateDistance("موتورسیکلت", ObjectCategory.HAZARD, motorBox)
        assertTrue("Distance should be within realistic range", distance in 1.5f..2.5f)

        val moneyBox = RectF(0.2f, 0.3f, 0.8f, 0.7f) // height = 0.4f
        val moneyDist = tracker.estimateDistance("اسکناس ۱۰۰ هزار تومانی", ObjectCategory.CURRENCY, moneyBox)
        assertTrue("Banknote close distance expected", moneyDist < 0.5f)
    }

    @Test
    fun `test kinematic tracking and collision alert`() {
        val initialBox = RectF(0.3f, 0.2f, 0.6f, 0.5f)
        val t0 = 100000L

        val r0 = tracker.trackKinematics("obj_bike_test", initialBox, 6.0f, t0)
        assertEquals(0f, r0.speedKmh, 0.01f)

        val approachingBox = RectF(0.25f, 0.15f, 0.75f, 0.85f)
        val t1 = t0 + 300L
        val r1 = tracker.trackKinematics("obj_bike_test", approachingBox, 1.5f, t1)

        assertTrue(r1.isApproaching)
        assertTrue(r1.isCollisionRisk)
        assertNotNull(r1.timeToCollision)
    }

    @Test
    fun `test dataset augmentation engine creates diverse variations`() = runBlocking {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val augmented = augmentationEngine.generateAugmentations(testBmp)
        assertTrue("Should produce multiple visual variations", augmented.size >= 5)

        val titles = augmented.map { it.titleFa }
        assertTrue(titles.any { it.contains("افقی") })
        assertTrue(titles.any { it.contains("آفتاب") })
        assertTrue(titles.any { it.contains("تاریکی") })
        assertTrue(titles.any { it.contains("لرزش") })
    }

    @Test
    fun `test custom trained repository and on-device training`() = runBlocking {
        val sampleBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val trained = trainer.trainCustomModel(
            objectNameFa = "قرص لوزارتان ۲۵",
            category = ObjectCategory.DAILY_ITEM,
            physicalHeightMeters = 0.05f,
            isCollisionThreat = false,
            originalImages = listOf(sampleBmp),
            augmentedImages = listOf(sampleBmp, sampleBmp),
            onProgress = {}
        )

        assertNotNull(trained)
        assertEquals("قرص لوزارتان ۲۵", trained.nameFa)
        assertTrue(trained.accuracyScore > 0.90f)

        val detections = CustomTrainedObjectRepository.createDetectionsFromCustomClasses()
        assertTrue(detections.any { it.labelFa == "قرص لوزارتان ۲۵" })
    }

    @Test
    fun `test iran dataset repository integrity`() {
        val scenarios = IranDatasetRepository.scenarios
        assertTrue(scenarios.isNotEmpty())

        val motorScenario = scenarios.firstOrNull { it.id == "motor_sidewalk" }
        assertNotNull(motorScenario)
        assertEquals(SafetyStatus.CRITICAL, motorScenario?.safetyStatus)

        val joubScenario = scenarios.firstOrNull { it.id == "joub_and_bridge" }
        assertNotNull(joubScenario)

        val moneyScenario = scenarios.firstOrNull { it.id == "iran_banknotes" }
        assertNotNull(moneyScenario)
    }

    @Test
    fun `test espeak ng phoneme transliterator`() {
        val espeak = com.example.service.EspeakNgVoiceSynthesizer()
        val phonemes = espeak.textToPhonemes("خطر فوری موتورسیکلت در پیاده‌رو")
        assertTrue(phonemes.isNotBlank())
        assertTrue(phonemes.contains("xatar"))
        assertTrue(phonemes.contains("fowri"))
        assertTrue(phonemes.contains("motorsiklet"))
    }

    @Test
    fun `test persian guidance algorithm priority sorting`() {
        val collisionObj = com.example.model.DetectedObject(
            id = "c1",
            labelFa = "موتورسیکلت",
            labelEn = "Motorcycle",
            category = ObjectCategory.HAZARD,
            boundingBox = RectF(0.2f, 0.2f, 0.5f, 0.7f),
            confidence = 0.98f,
            distanceMeters = 1.8f,
            speedKmh = 25f,
            velocityVector = Pair(0.1f, 0.3f),
            trajectoryNameFa = "به سمت شما",
            isApproaching = true,
            isCollisionRisk = true,
            clockPosition = 12
        )

        val guidanceCrit = com.example.service.PersianGuidanceAlgorithm.generatePrioritizedGuidance(
            listOf(collisionObj),
            SafetyStatus.CRITICAL
        )
        assertEquals(com.example.service.PersianGuidanceAlgorithm.GuidanceLevel.CRITICAL, guidanceCrit.level)
        assertTrue(guidanceCrit.isUrgent)
        assertTrue(guidanceCrit.speechText.contains("خطر فوری"))
        assertTrue(guidanceCrit.speechText.contains("موتورسیکلت"))

        val guidanceEmpty = com.example.service.PersianGuidanceAlgorithm.generatePrioritizedGuidance(
            emptyList(),
            SafetyStatus.SAFE
        )
        assertEquals(com.example.service.PersianGuidanceAlgorithm.GuidanceLevel.ALL_CLEAR, guidanceEmpty.level)
        assertTrue(guidanceEmpty.speechText.contains("باز و آزاد است"))
    }
}
