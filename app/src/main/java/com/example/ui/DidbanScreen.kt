package com.example.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppMode
import com.example.model.DetectedObject
import com.example.model.SafetyStatus
import com.example.service.CameraManager
import com.example.service.IranDatasetRepository
import com.example.service.IranScenario
import com.example.service.VoiceEngineType
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.viewmodel.DidbanViewModel

@Composable
fun DidbanScreen(
    viewModel: DidbanViewModel,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentMode by viewModel.currentMode.collectAsState()
    val scenePerception by viewModel.scenePerception.collectAsState()
    val selectedScenario by viewModel.selectedScenario.collectAsState()
    val isSpeechEnabled by viewModel.isSpeechEnabled.collectAsState()
    val isSonarMuted by viewModel.isSonarMuted.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiDetailedAnalysis by viewModel.aiDetailedAnalysis.collectAsState()

    var isTorchOn by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    val cameraManager = remember { CameraManager(context) }

    // RTL for Persian layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showVoiceSettingsDialog) {
            VoiceSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showVoiceSettingsDialog = false }
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground),
            containerColor = ObsidianBackground,
            topBar = {
                DidbanTopBar(
                    fps = scenePerception.fps,
                    latencyMs = scenePerception.inferenceLatencyMs,
                    isSpeechEnabled = isSpeechEnabled,
                    isSonarMuted = isSonarMuted,
                    isTorchOn = isTorchOn,
                    onToggleSpeech = { viewModel.toggleSpeech() },
                    onToggleSonar = { viewModel.toggleSonar() },
                    onToggleTorch = {
                        isTorchOn = cameraManager.toggleTorch()
                    },
                    onOpenVoiceSettings = {
                        showVoiceSettingsDialog = true
                    }
                )
            },
            bottomBar = {
                DidbanBottomNavigation(
                    currentMode = currentMode,
                    onSelectMode = { viewModel.setMode(it) }
                )
            }
        ) { paddingValues ->
            if (currentMode == AppMode.TRAIN_MODEL) {
                Box(modifier = Modifier.padding(paddingValues)) {
                    ModelTrainingScreen(
                        viewModel = viewModel,
                        onNavigateToRadar = { viewModel.setMode(AppMode.RADAR) }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Urgent Collision Alert Banner
                AnimatedVisibility(
                    visible = scenePerception.overallSafety == SafetyStatus.CRITICAL,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CollisionAlertBanner(
                        alertText = scenePerception.immediateAudioAlertFa,
                        modifier = Modifier.testTag("collision_alert_banner")
                    )
                }

                // Main Interactive Viewport & Radar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.triggerInstantAudioReport() },
                                onDoubleTap = { viewModel.triggerDeepHumanEyeAnalysis() }
                            )
                        }
                        .testTag("viewport_touch_area")
                ) {
                    // CameraX preview
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).also { previewView ->
                                    cameraManager.startCamera(
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = previewView,
                                        onFrameAnalyzed = { lum, bmp ->
                                            viewModel.onCameraFrame(lum, bmp)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // High-tech Sonar Grid Background when camera permission pending or in simulation
                        SonarSimulatedBackground()
                    }

                    // Augmented Reality HUD & Kinematics Overlay
                    RadarCanvasOverlay(
                        objects = scenePerception.objects,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Ambient Light & Status Pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scenePerception.ambientLight,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Touch Gesture Instructions for the Blind
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "ضربه: اعلام فوری موانع | دو ضربه: تحلیل چشم انسان",
                            color = AmberPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Scenario Carousel for Iranian Datasets & Street Hazards
                IranianScenarioCarousel(
                    scenarios = IranDatasetRepository.scenarios,
                    selectedScenario = selectedScenario,
                    onSelect = { viewModel.loadScenario(it) }
                )

                // Objects Info List / AI Analysis Card
                if (isAiLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "هوش مصنوعی در حال تحلیل بینایی مانند چشم انسان...",
                                color = AmberPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (aiDetailedAnalysis != null) {
                    AiDescriptionCard(
                        text = aiDetailedAnalysis!!,
                        onReAnalyze = { viewModel.triggerDeepHumanEyeAnalysis() }
                    )
                } else {
                    DetectedObjectsSummaryList(objects = scenePerception.objects)
                }
            }
        }
    }
}
}

@Composable
fun DidbanTopBar(
    fps: Int,
    latencyMs: Long,
    isSpeechEnabled: Boolean,
    isSonarMuted: Boolean,
    isTorchOn: Boolean,
    onToggleSpeech: () -> Unit,
    onToggleSonar: () -> Unit,
    onToggleTorch: () -> Unit,
    onOpenVoiceSettings: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "آیکون رادار",
                        tint = AmberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دیدبان نابینایان",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = SafeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "YOLO v11 • $fps FPS • ${latencyMs}ms",
                            color = SafeGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "دیتاست ایران",
                        color = RadarCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.testTag("btn_toggle_torch")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                        contentDescription = "چراغ قوه",
                        tint = if (isTorchOn) AmberPrimary else Color.Gray
                    )
                }

                IconButton(
                    onClick = onToggleSonar,
                    modifier = Modifier.testTag("btn_toggle_sonar")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = if (!isSonarMuted) "سونار صوتی (روشن)" else "سونار صوتی (خاموش - برای فعال‌سازی لمس کنید)",
                        tint = if (!isSonarMuted) RadarCyan else Color.Gray
                    )
                }

                IconButton(
                    onClick = onOpenVoiceSettings,
                    modifier = Modifier.testTag("btn_open_voice_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "تنظیمات گوینده صوتی eSpeak-NG",
                        tint = AmberPrimary
                    )
                }

                IconButton(
                    onClick = onToggleSpeech,
                    modifier = Modifier.testTag("btn_toggle_speech")
                ) {
                    Icon(
                        imageVector = if (isSpeechEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "صدای راهنما",
                        tint = if (isSpeechEnabled) AmberPrimary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CollisionAlertBanner(alertText: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DangerRedContainer),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, DangerRed)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "هشدار خطر",
                tint = DangerRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = alertText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun SonarSimulatedBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = null,
                tint = AmberPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "اسکنر پرسرعت خیابان و پیاده‌رو",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تشخیص حرکت، سرعت و فواصل در حال اجراست",
                color = RadarCyan.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun IranianScenarioCarousel(
    scenarios: List<IranScenario>,
    selectedScenario: IranScenario,
    onSelect: (IranScenario) -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            text = "انتخاب سناریوی زندگی روزمره در ایران:",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(scenarios) { scenario ->
                val isSelected = scenario.id == selectedScenario.id
                Surface(
                    color = if (isSelected) AmberPrimary else SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E384D)),
                    modifier = Modifier
                        .clickable { onSelect(scenario) }
                        .testTag("scenario_${scenario.id}")
                ) {
                    Text(
                        text = scenario.titleFa,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetectedObjectsSummaryList(objects: List<DetectedObject>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(objects) { obj ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (obj.isCollisionRisk) DangerRedContainer else SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (obj.isCollisionRisk) DangerRed else AmberPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${obj.clockPosition}",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = obj.labelFa,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${obj.trajectoryNameFa} (${obj.colorFa})",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${obj.distanceMeters} متر",
                            color = if (obj.isCollisionRisk) DangerRed else RadarCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (obj.speedKmh > 0f) {
                            Text(
                                text = "${obj.speedKmh.toInt()} km/h",
                                color = AmberPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiDescriptionCard(text: String, onReAnalyze: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دید عمیق چشم انسان (Gemini AI):",
                    color = AmberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onReAnalyze,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("اسکن مجدد", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DidbanBottomNavigation(
    currentMode: AppMode,
    onSelectMode: (AppMode) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val items = listOf(
            Triple(AppMode.RADAR, "رادار موانع", Icons.Default.Radar),
            Triple(AppMode.IRAN_DATASET, "اشیاء ایران", Icons.Default.Navigation),
            Triple(AppMode.CURRENCY_SCAN, "اسکناس و دارو", Icons.Default.Campaign),
            Triple(AppMode.HUMAN_EYE_AI, "چشم انسان", Icons.Default.Psychology),
            Triple(AppMode.TRAIN_MODEL, "آموزش مدل", Icons.Default.Tune)
        )

        items.forEach { (mode, label, icon) ->
            val isSelected = currentMode == mode
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectMode(mode) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.Black else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) AmberPrimary else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AmberPrimary
                ),
                modifier = Modifier.testTag("nav_${mode.name.lowercase()}")
            )
        }
    }
}

@Composable
fun VoiceSettingsDialog(
    viewModel: DidbanViewModel,
    onDismiss: () -> Unit
) {
    val activeEngine by viewModel.voiceEngineType.collectAsState()
    val volumeBoost by viewModel.volumeBoost.collectAsState()
    val isPersian by viewModel.isPersianLanguage.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنظیمات گوینده صوتی دیدبان",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "موتور صوتی تبدیل متن به گفتار:",
                    color = AmberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = if (activeEngine == VoiceEngineType.ESPEAK_NG) AmberPrimary.copy(alpha = 0.2f) else SurfaceVariantDark,
                    shape = RoundedCornerShape(10.dp),
                    border = if (activeEngine == VoiceEngineType.ESPEAK_NG) androidx.compose.foundation.BorderStroke(1.5.dp, AmberPrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setVoiceEngine(VoiceEngineType.ESPEAK_NG) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "موتور داخلی eSpeak-NG (پیش‌فرض)",
                            color = if (activeEngine == VoiceEngineType.ESPEAK_NG) AmberPrimary else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "۱۰۰٪ آفلاین، تلفظ دقیق واج‌های فارسی، بدون تاخیر و با صدای بسیار بلند",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = if (activeEngine == VoiceEngineType.ANDROID_SYSTEM) AmberPrimary.copy(alpha = 0.2f) else SurfaceVariantDark,
                    shape = RoundedCornerShape(10.dp),
                    border = if (activeEngine == VoiceEngineType.ANDROID_SYSTEM) androidx.compose.foundation.BorderStroke(1.5.dp, AmberPrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setVoiceEngine(VoiceEngineType.ANDROID_SYSTEM) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "موتور سیستمی اندروید (Google TTS)",
                            color = if (activeEngine == VoiceEngineType.ANDROID_SYSTEM) AmberPrimary else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "استفاده از پکیج صدای نصب‌شده در سیستم گوشی",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تقویت حجم صدا برای خیابان:",
                            color = AmberPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(volumeBoost * 100).toInt()}%",
                            color = RadarCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Slider(
                        value = volumeBoost,
                        onValueChange = { viewModel.setVolumeBoost(it) },
                        valueRange = 1.0f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "زبان گفتار:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = if (isPersian) AmberPrimary else SurfaceVariantDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.setLanguage(true) }
                        ) {
                            Text(
                                text = "فارسی",
                                color = if (isPersian) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            color = if (!isPersian) AmberPrimary else SurfaceVariantDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.setLanguage(false) }
                        ) {
                            Text(
                                text = "English",
                                color = if (!isPersian) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.triggerInstantAudioReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = RadarCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تست گفتار و اولویت‌بندی موانع", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("تأیید و ذخیره", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
