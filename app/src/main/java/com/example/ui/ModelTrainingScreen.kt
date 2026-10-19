package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.model.CustomTrainedClass
import com.example.model.ObjectCategory
import com.example.service.AugmentedSample
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.viewmodel.DidbanViewModel

@Composable
fun ModelTrainingScreen(
    viewModel: DidbanViewModel,
    onNavigateToRadar: () -> Unit
) {
    val context = LocalContext.current

    val datasetImages by viewModel.datasetImages.collectAsState()
    val augmentedSamples by viewModel.augmentedSamples.collectAsState()
    val trainingProgress by viewModel.trainingProgress.collectAsState()
    val isCollectingData by viewModel.isCollectingData.collectAsState()
    val customClasses by viewModel.customTrainedClasses.collectAsState()

    var objectNameFa by remember { mutableStateOf("میله زرد پیاده‌رو جدید") }
    var selectedCategory by remember { mutableStateOf(ObjectCategory.INFRASTRUCTURE) }
    var searchQuery by remember { mutableStateOf("میله زرد پیاده رو تهران") }
    var isThreat by remember { mutableStateOf(false) }
    var heightMeters by remember { mutableStateOf("0.85") }

    // Android Photo Picker for zero-permission gallery importing (compliant with Google Play Policy)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesFromGalleryUris(context, uris)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AmberPrimary.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "استودیوی بهبود و آموزش مدل",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "دانلود خودکار از گوگل بدون API، گالری، افکت‌ها و آموزش اختصاصی",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section 1: Definition of Object
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "۱. مشخصات شیء جهت آموزش:",
                        color = AmberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = objectNameFa,
                        onValueChange = { objectNameFa = it },
                        label = { Text("نام شیء (مثلاً: داروی لوزارتان، اسکناس ۲۰۰ تومانی)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_object_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مانع خطرناک و هشدار برخورد",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "در صورت نزدیک شدن، بوق رادار و ویبره فعال شود",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isThreat,
                            onCheckedChange = { isThreat = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberPrimary,
                                checkedTrackColor = AmberPrimary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Acquisition (Google search without API / Gallery / Camera)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "۲. جمع‌آوری تصاویر دیتاست:",
                        color = AmberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Google Image Search without API
                    Text(
                        text = "الف) دانلود خودکار از نمایش بندانگشتی گوگل (بدون API):",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("عبارت جستجو در گوگل...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_google_search"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RadarCyan,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.searchAndDownloadGoogleImages(searchQuery, 40)
                            },
                            enabled = !isCollectingData,
                            colors = ButtonDefaults.buttonColors(containerColor = RadarCyan),
                            modifier = Modifier.testTag("btn_search_google")
                        ) {
                            if (isCollectingData) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دانلود", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Quick suggestion pills
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val suggestions = listOf("میله زرد پیاده رو", "اسکناس صد هزار تومانی", "جعبه داروی ایرانی", "جوب آب خیابان")
                        items(suggestions) { s ->
                            Surface(
                                color = SurfaceVariantDark,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    searchQuery = s
                                    viewModel.searchAndDownloadGoogleImages(s, 40)
                                }
                            ) {
                                Text(
                                    text = s,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gallery & Camera options
                    Text(
                        text = "ب) یا افزودن از گالری و دوربین:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_pick_gallery")
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتخاب از گالری", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                // Synthesizes camera capture sample
                                val syntheticCamera = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                                viewModel.addCameraSnapshot(syntheticCamera)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_capture_camera")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عکاسی مستقیم", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Dataset count badge
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (datasetImages.isNotEmpty()) SafeGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (datasetImages.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (datasetImages.isNotEmpty()) SafeGreen else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (datasetImages.isNotEmpty())
                                    "تعداد تصاویر دریافت شده: ${datasetImages.size} تصویر آماده ارتقا"
                                else
                                    "هنوز تصویری اضافه نشده است. یک روش را انتخاب کنید.",
                                color = if (datasetImages.isNotEmpty()) SafeGreen else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Data Augmentation Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "۳. افکت‌ها و ارتقای تصویر (Augmentation):",
                                color = AmberPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (augmentedSamples.isNotEmpty()) {
                            Text(
                                text = "${augmentedSamples.size} نمونه افکت",
                                color = RadarCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای این که مدل در نور آفتاب شدید، تاریکی شب و لرزش دست نابینا خطا ندهد، این افکت‌ها به‌صورت خودکار اعمال می‌شوند:",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (augmentedSamples.isEmpty()) {
                        Text(
                            text = "پس از دریافت تصاویر، افکت‌ها در اینجا نمایش می‌یابند.",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(augmentedSamples) { sample ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Image(
                                        bitmap = sample.bitmap.asImageBitmap(),
                                        contentDescription = sample.titleFa,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sample.titleFa,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Training Pipeline (Epochs & Loss curve)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "۴. فرآیند آموزش روی مدل (Fine-Tuning):",
                        color = AmberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!trainingProgress.isRunning && !trainingProgress.isFinished) {
                        Button(
                            onClick = {
                                val h = heightMeters.toFloatOrNull() ?: 0.85f
                                viewModel.startModelTraining(
                                    objectNameFa = objectNameFa,
                                    category = selectedCategory,
                                    physicalHeightMeters = h,
                                    isCollisionThreat = isThreat
                                )
                            },
                            enabled = datasetImages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_training")
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شروع آموزش مدل روی این شیء",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        // Progress & Metrics Card
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "اپوک: ${trainingProgress.currentEpoch} از ${trainingProgress.totalEpochs}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "دقت (mAP): ${(trainingProgress.accuracy * 100).toInt()}%",
                                    color = SafeGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { trainingProgress.currentEpoch.toFloat() / trainingProgress.totalEpochs },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AmberPrimary,
                                trackColor = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = trainingProgress.currentStageFa,
                                    color = RadarCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Loss: ${trainingProgress.loss}",
                                    color = AmberPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (trainingProgress.isFinished) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onNavigateToRadar,
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_test_in_radar")
                                ) {
                                    Text(
                                        text = "تست فوری شیء آموزش‌دیده در رادار دیدبان",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: List of Custom Trained Objects
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "۵. اشیاء آموزش‌دیده ذخیره‌شده روی دستگاه:",
                        color = AmberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (customClasses.isEmpty()) {
                        Text(
                            text = "هنوز شیء اختصاصی آموزش داده نشده است. اشیاء پس از آموزش در رادار زنده فعال می‌شوند.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        customClasses.forEach { cls ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = cls.nameFa,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "دقت: ${(cls.accuracyScore * 100).toInt()}% • تصاویر: ${cls.originalImagesCount + cls.augmentedImagesCount}",
                                            color = SafeGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeTrainedCustomClass(cls.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
