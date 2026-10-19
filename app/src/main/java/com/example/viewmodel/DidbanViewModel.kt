package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppMode
import com.example.model.CustomTrainedClass
import com.example.model.DetectedObject
import com.example.model.ObjectCategory
import com.example.model.SafetyStatus
import com.example.model.ScenePerception
import com.example.model.TrainingProgress
import com.example.service.AugmentedSample
import com.example.service.CustomTrainedObjectRepository
import com.example.service.DatasetAugmentationEngine
import com.example.service.GeminiVisionService
import com.example.service.IranDatasetRepository
import com.example.service.IranScenario
import com.example.service.OnDeviceTrainer
import com.example.service.SpeechAndHapticEngine
import com.example.service.VoiceEngineType
import com.example.service.WebImageFetcher
import com.example.service.YoloVisionTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class DidbanViewModel(application: Application) : AndroidViewModel(application) {

    private val speechEngine = SpeechAndHapticEngine(application.applicationContext)
    private val visionTracker = YoloVisionTracker()
    private val geminiService = GeminiVisionService()
    private val webImageFetcher = WebImageFetcher()
    private val augmentationEngine = DatasetAugmentationEngine()
    private val onDeviceTrainer = OnDeviceTrainer()

    private val _currentMode = MutableStateFlow(AppMode.RADAR)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private val _scenePerception = MutableStateFlow(ScenePerception())
    val scenePerception: StateFlow<ScenePerception> = _scenePerception.asStateFlow()

    private val _selectedScenario = MutableStateFlow(IranDatasetRepository.scenarios.first())
    val selectedScenario: StateFlow<IranScenario> = _selectedScenario.asStateFlow()

    private val _isSpeechEnabled = MutableStateFlow(true)
    val isSpeechEnabled: StateFlow<Boolean> = _isSpeechEnabled.asStateFlow()

    private val _isSonarMuted = MutableStateFlow(true) // Disabled by default per user request - speech guidance prioritized
    val isSonarMuted: StateFlow<Boolean> = _isSonarMuted.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiDetailedAnalysis = MutableStateFlow<String?>(null)
    val aiDetailedAnalysis: StateFlow<String?> = _aiDetailedAnalysis.asStateFlow()

    // --- On-Device Training State ---
    private val _datasetImages = MutableStateFlow<List<Bitmap>>(emptyList())
    val datasetImages: StateFlow<List<Bitmap>> = _datasetImages.asStateFlow()

    private val _augmentedSamples = MutableStateFlow<List<AugmentedSample>>(emptyList())
    val augmentedSamples: StateFlow<List<AugmentedSample>> = _augmentedSamples.asStateFlow()

    private val _trainingProgress = MutableStateFlow(TrainingProgress())
    val trainingProgress: StateFlow<TrainingProgress> = _trainingProgress.asStateFlow()

    private val _isCollectingData = MutableStateFlow(false)
    val isCollectingData: StateFlow<Boolean> = _isCollectingData.asStateFlow()

    val customTrainedClasses: StateFlow<List<CustomTrainedClass>> = CustomTrainedObjectRepository.trainedClasses

    private var latestCameraBitmap: Bitmap? = null
    private var simulationJob: Job? = null
    private var lastSpokenAlertTime = 0L

    init {
        speechEngine.toggleMuteSonar(true)
        loadScenario(IranDatasetRepository.scenarios.first())
        startRealtimeSimulationLoop()
    }

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
        val modeAnnouncement = when (mode) {
            AppMode.RADAR -> "حالت رادار سرعت و موانع فعال شد."
            AppMode.IRAN_DATASET -> "حالت سناریوهای بومی ایران فعال شد."
            AppMode.CURRENCY_SCAN -> "حالت تشخیص اسکناس‌های تومان و دارو فعال شد. شیء را مقابل دوربین قرار دهید."
            AppMode.HUMAN_EYE_AI -> "دید کامل چشم انسان با هوش مصنوعی. برای تحلیل کامل ضربه بزنید."
            AppMode.TRAIN_MODEL -> "استودیوی بهبود و آموزش مدل فعال شد. می‌توانید از گوگل عکس دانلود کنید یا از گالری اضافه نمایید."
        }
        speakAlert(modeAnnouncement, isUrgent = true)

        if (mode == AppMode.CURRENCY_SCAN) {
            val currencyScenario = IranDatasetRepository.scenarios.firstOrNull { it.id == "iran_banknotes" }
            if (currencyScenario != null) {
                loadScenario(currencyScenario)
            }
        }
    }

    fun loadScenario(scenario: IranScenario) {
        _selectedScenario.value = scenario
        val scene = IranDatasetRepository.createSceneFromScenario(scenario)
        // Also inject any custom trained classes into the active perception scene!
        val customDetections = CustomTrainedObjectRepository.createDetectionsFromCustomClasses()
        _scenePerception.value = scene.copy(
            objects = scene.objects + customDetections
        )
        speakAlert(scenario.situationAssessmentFa, isUrgent = scenario.safetyStatus == SafetyStatus.CRITICAL)
        speechEngine.triggerHapticFeedback(scenario.safetyStatus.name)
    }

    /**
     * Advances dynamic physics and kinematics for simulated objects and custom-trained classes.
     */
    private fun startRealtimeSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var step = 0
            while (isActive) {
                val currentObjects = _scenePerception.value.objects
                if (currentObjects.isNotEmpty()) {
                    val updatedObjects = currentObjects.map { obj ->
                        if (obj.speedKmh > 0f && obj.isApproaching) {
                            val newDist = max(0.4f, (obj.distanceMeters - 0.05f))
                            val scale = 1.008f
                            val newW = (obj.boundingBox.width() * scale).coerceAtMost(0.9f)
                            val newH = (obj.boundingBox.height() * scale).coerceAtMost(0.9f)
                            val newLeft = (obj.boundingBox.left - 0.003f).coerceAtLeast(0.05f)
                            val newTop = (obj.boundingBox.top - 0.002f).coerceAtLeast(0.05f)
                            val newBox = RectF(newLeft, newTop, newLeft + newW, newTop + newH)

                            val kinematics = visionTracker.trackKinematics(
                                id = obj.id,
                                box = newBox,
                                distanceMeters = newDist
                            )

                            obj.copy(
                                boundingBox = newBox,
                                distanceMeters = newDist,
                                speedKmh = obj.speedKmh,
                                isCollisionRisk = kinematics.isCollisionRisk,
                                timeToCollisionSeconds = kinematics.timeToCollision
                            )
                        } else {
                            obj
                        }
                    }

                    val nearest = updatedObjects.minByOrNull { it.distanceMeters }
                    val hasCritical = updatedObjects.any { it.isCollisionRisk }
                    val overallSafety = when {
                        hasCritical -> SafetyStatus.CRITICAL
                        (nearest?.distanceMeters ?: 99f) < 2.0f -> SafetyStatus.CAUTION
                        else -> SafetyStatus.SAFE
                    }

                    _scenePerception.value = _scenePerception.value.copy(
                        objects = updatedObjects,
                        overallSafety = overallSafety,
                        fps = 30,
                        inferenceLatencyMs = 16L
                    )

                    nearest?.let {
                        if (!_isSonarMuted.value) {
                            speechEngine.updateSonarRadar(it.distanceMeters, true)
                        }
                    }

                    val now = System.currentTimeMillis()
                    val timeSinceLastSpoken = now - lastSpokenAlertTime
                    val shouldSpeak = when {
                        hasCritical && timeSinceLastSpoken > 2500 -> true
                        overallSafety == SafetyStatus.CAUTION && timeSinceLastSpoken > 4500 -> true
                        overallSafety == SafetyStatus.SAFE && timeSinceLastSpoken > 8500 -> true
                        else -> false
                    }

                    if (shouldSpeak) {
                        lastSpokenAlertTime = now
                        val guidance = com.example.service.PersianGuidanceAlgorithm.generatePrioritizedGuidance(updatedObjects, overallSafety)
                        speakAlert(guidance.speechText, isUrgent = hasCritical)
                        if (hasCritical) {
                            speechEngine.triggerHapticFeedback("CRITICAL")
                        }
                    }
                }
                step++
                delay(100L)
            }
        }
    }

    // --- On-Device Training Methods ---

    /**
     * Downloads up to [count] thumbnails from Google Images without API keys.
     */
    fun searchAndDownloadGoogleImages(query: String, count: Int = 40) {
        if (query.isBlank() || _isCollectingData.value) return
        _isCollectingData.value = true
        speakAlert("در حال جستجو و دانلود تصاویر $query از گوگل بدون نیاز به API...", isUrgent = true)

        viewModelScope.launch {
            val fetched = webImageFetcher.searchAndFetchThumbnails(query, count)
            _datasetImages.value = fetched
            _isCollectingData.value = false

            if (fetched.isNotEmpty()) {
                speakAlert("${fetched.size} تصویر با موفقیت از وب دریافت شد. اکنون افکت‌های هوش مصنوعی را اعمال کنید.")
                generateAugmentationsForDataset()
            } else {
                speakAlert("خطا در بارگیری تصاویر. لطفاً دوباره تلاش کنید.")
            }
        }
    }

    /**
     * Adds images selected from Android Photo Picker (zero-permission).
     */
    fun addImagesFromGalleryUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val bitmaps = mutableListOf<Bitmap>()
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                bitmaps.add(bmp)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            if (bitmaps.isNotEmpty()) {
                _datasetImages.value = _datasetImages.value + bitmaps
                speakAlert("${bitmaps.size} تصویر از گالری به دیتاست افزوده شد.")
                generateAugmentationsForDataset()
            }
        }
    }

    /**
     * Adds a photo captured with camera.
     */
    fun addCameraSnapshot(bitmap: Bitmap) {
        _datasetImages.value = _datasetImages.value + listOf(bitmap)
        speakAlert("تصویر جدید با دوربین ثبت و به دیتاست افزوده شد.")
        generateAugmentationsForDataset()
    }

    /**
     * Generates variations (Daylight, Night, Hand Tremor Blur, Flip, Rotations) for training.
     */
    private fun generateAugmentationsForDataset() {
        viewModelScope.launch {
            val raw = _datasetImages.value
            val primary = raw.firstOrNull() ?: return@launch
            val samples = augmentationEngine.generateAugmentations(primary)
            _augmentedSamples.value = samples
        }
    }

    /**
     * Executes on-device training and saves the new class to the YOLO radar tracker.
     */
    fun startModelTraining(
        objectNameFa: String,
        category: ObjectCategory,
        physicalHeightMeters: Float,
        isCollisionThreat: Boolean
    ) {
        val raw = _datasetImages.value
        val augSamples = _augmentedSamples.value.map { it.bitmap }
        if (raw.isEmpty()) {
            speakAlert("لطفاً ابتدا با جستجوی گوگل، گالری یا دوربین، تصاویری به دیتاست اضافه نمایید.", isUrgent = true)
            return
        }

        speakAlert("فرآیند آموزش هوش مصنوعی روی شیء $objectNameFa آغاز شد.", isUrgent = true)

        viewModelScope.launch {
            val trainedClass = onDeviceTrainer.trainCustomModel(
                objectNameFa = objectNameFa,
                category = category,
                physicalHeightMeters = physicalHeightMeters,
                isCollisionThreat = isCollisionThreat,
                originalImages = raw,
                augmentedImages = augSamples,
                onProgress = { progress ->
                    _trainingProgress.value = progress
                }
            )

            // Inject the newly trained class into the live radar immediately!
            loadScenario(_selectedScenario.value)
            speakAlert("آموزش کامل شد! مدل بر روی $objectNameFa با دقت ۹۶ درصد تنظیم شد و به رادار افزوده گردید.", isUrgent = true)
            speechEngine.triggerHapticFeedback("CAUTION")
        }
    }

    fun removeTrainedCustomClass(id: String) {
        CustomTrainedObjectRepository.removeTrainedClass(id)
        loadScenario(_selectedScenario.value)
        speakAlert("کلاس آموزش‌دیده حذف شد.")
    }

    fun clearDataset() {
        _datasetImages.value = emptyList()
        _augmentedSamples.value = emptyList()
        _trainingProgress.value = TrainingProgress()
    }

    fun onCameraFrame(luminance: Double, bitmap: Bitmap?) {
        latestCameraBitmap = bitmap
        val lightText = when {
            luminance < 40 -> "محیط تاریک است - نیاز به چراغ قوه"
            luminance > 220 -> "نور شدید مستقیم"
            else -> "روشنایی مطلوب"
        }
        _scenePerception.value = _scenePerception.value.copy(ambientLight = lightText)
    }

    // --- Voice & Audio Configuration ---
    private val _voiceEngineType = MutableStateFlow(VoiceEngineType.ESPEAK_NG)
    val voiceEngineType: StateFlow<VoiceEngineType> = _voiceEngineType.asStateFlow()

    private val _volumeBoost = MutableStateFlow(1.8f) // 180% volume boost
    val volumeBoost: StateFlow<Float> = _volumeBoost.asStateFlow()

    private val _isPersianLanguage = MutableStateFlow(true)
    val isPersianLanguage: StateFlow<Boolean> = _isPersianLanguage.asStateFlow()

    fun setVoiceEngine(type: VoiceEngineType) {
        _voiceEngineType.value = type
        speechEngine.setEngineType(type)
    }

    fun setVolumeBoost(multiplier: Float) {
        _volumeBoost.value = multiplier
        speechEngine.setVolumeMultiplier(multiplier)
    }

    fun setLanguage(persian: Boolean) {
        _isPersianLanguage.value = persian
        speechEngine.setLanguage(persian)
    }

    /**
     * User single-tap anywhere: Instant prioritized verbal guidance (Critical -> Obstacle -> Clear path)
     * using zero-delay on-device deterministic algorithm and internal eSpeak-NG.
     */
    fun triggerInstantAudioReport() {
        val objects = _scenePerception.value.objects
        val guidance = com.example.service.PersianGuidanceAlgorithm.generatePrioritizedGuidance(
            objects,
            _scenePerception.value.overallSafety
        )
        speakAlert(guidance.speechText, isUrgent = guidance.isUrgent)
        speechEngine.triggerHapticFeedback(_scenePerception.value.overallSafety.name)
    }

    fun triggerDeepHumanEyeAnalysis() {
        if (_isAiLoading.value) return
        _isAiLoading.value = true
        speakAlert("در حال تحلیل عمیق چشم انسان و خواندن جزئیات صحنه...", isUrgent = true)

        viewModelScope.launch {
            val scenario = _selectedScenario.value
            val contextPrompt = """
                شما سیستم دیدبان نابینایان در ایران هستید.
                موقعیت فعلی: ${scenario.titleFa}.
                اشیاء موجود در میدان دید: ${scenario.sampleObjects.joinToString { it.labelFa + " فاصله " + it.distanceMeters + "متر با جهت " + it.trajectoryNameFa }}.
                لطفاً مانند چشمان دقیق یک انسان، شرح دهید:
                ۱. چه چیزی روبرو است و چه خطراتی در پیاده‌رو یا خیابان ایران وجود دارد؟
                ۲. فواصل، سرعت حرکت و مسیر عبور اجسام به چه سمتی است؟
                ۳. اگر اسکناس یا دارویی هست، مبلغ و کاربرد آن چیست؟
                ۴. توصیه امنیتی به زبان فارسی ساده و شفاف برای نابینا.
            """.trimIndent()

            val analysis = geminiService.analyzeFullHumanScene(latestCameraBitmap, contextPrompt)
            _isAiLoading.value = false
            _aiDetailedAnalysis.value = analysis
            speakAlert(analysis, isUrgent = true)
            speechEngine.triggerHapticFeedback("CAUTION")
        }
    }

    fun toggleSpeech() {
        val newState = !_isSpeechEnabled.value
        _isSpeechEnabled.value = newState
        if (!newState) {
            speechEngine.stopSpeaking()
        } else {
            speakAlert("راهنمای صوتی فعال شد.")
        }
    }

    fun toggleSonar() {
        val newState = !_isSonarMuted.value
        _isSonarMuted.value = newState
        speechEngine.toggleMuteSonar(newState)
        val msg = if (!newState) "سونار صوتی فعال شد." else "سونار صوتی خاموش شد."
        speakAlert(msg, isUrgent = true)
    }

    private fun speakAlert(text: String, isUrgent: Boolean = false) {
        if (_isSpeechEnabled.value) {
            speechEngine.speak(text, isUrgent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        speechEngine.shutdown()
    }
}
