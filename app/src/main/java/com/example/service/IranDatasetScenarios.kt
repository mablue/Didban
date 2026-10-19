package com.example.service

import android.graphics.RectF
import com.example.model.DetectedObject
import com.example.model.ObjectCategory
import com.example.model.SafetyStatus
import com.example.model.ScenePerception

data class IranScenario(
    val id: String,
    val titleFa: String,
    val descriptionFa: String,
    val category: ObjectCategory,
    val sampleObjects: List<DetectedObject>,
    val situationAssessmentFa: String,
    val safetyStatus: SafetyStatus
)

object IranDatasetRepository {

    val scenarios: List<IranScenario> = listOf(
        IranScenario(
            id = "motor_sidewalk",
            titleFa = "موتورسیکلت پرسرعت در پیاده‌رو",
            descriptionFa = "ورود ناگهانی موتورسیکلت هوندا ۱۲۵ به پیاده‌رو با سرعت بالا به سمت عابر",
            category = ObjectCategory.HAZARD,
            safetyStatus = SafetyStatus.CRITICAL,
            situationAssessmentFa = "خطر فوری! یک موتورسیکلت با سرعت ۲۸ کیلومتر بر ساعت از سمت چپ به طرف شما در حرکت است. برخورد ظرف ۱.۴ ثانیه. لطفاً به سمت راست بایستید.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_motor_1",
                    labelFa = "موتورسیکلت هوندا ۱۲۵",
                    labelEn = "Motorcycle (Honda 125)",
                    category = ObjectCategory.HAZARD,
                    boundingBox = RectF(0.22f, 0.30f, 0.58f, 0.82f),
                    confidence = 0.97f,
                    distanceMeters = 2.1f,
                    speedKmh = 28.5f,
                    velocityVector = Pair(0.12f, 0.35f),
                    trajectoryNameFa = "مستقیم به سمت شما با شتاب",
                    isApproaching = true,
                    isCollisionRisk = true,
                    timeToCollisionSeconds = 1.4f,
                    clockPosition = 11,
                    colorFa = "قرمز و مشکی",
                    detailedNoteFa = "موتور در خط مسیر مستقیم پیاده‌رو"
                ),
                DetectedObject(
                    id = "obj_curb_1",
                    labelFa = "لبه جدول پیاده‌رو",
                    labelEn = "Sidewalk Curb Edge",
                    category = ObjectCategory.INFRASTRUCTURE,
                    boundingBox = RectF(0.68f, 0.65f, 0.98f, 0.95f),
                    confidence = 0.92f,
                    distanceMeters = 1.6f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "ایستا / سمت راست",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 2,
                    colorFa = "بتنی خاکستری",
                    detailedNoteFa = "اختلاف سطح ۲۰ سانتی‌متر با جوب خیابان"
                )
            )
        ),
        IranScenario(
            id = "joub_and_bridge",
            titleFa = "جوب آب خیابان و پل فلزی باریک",
            descriptionFa = "تشخیص جوب عمیق روباز خیابان و موقعیت دقیق پل باریک فلزی جهت عبور ایمن",
            category = ObjectCategory.INFRASTRUCTURE,
            safetyStatus = SafetyStatus.CAUTION,
            situationAssessmentFa = "جوب روباز آب در فاصله ۱.۲ متری روبرو. پل فلزی باریک در ساعت ۱ (اندکی متمایل به راست) قرار دارد.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_joub_open",
                    labelFa = "جوب روباز خیابان",
                    labelEn = "Open Street Gutter / Canal",
                    category = ObjectCategory.INFRASTRUCTURE,
                    boundingBox = RectF(0.05f, 0.70f, 0.95f, 0.98f),
                    confidence = 0.95f,
                    distanceMeters = 1.2f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "مانع افقی در سراسر مسیر روبرو",
                    isApproaching = false,
                    isCollisionRisk = true,
                    timeToCollisionSeconds = null,
                    clockPosition = 12,
                    colorFa = "تیره / آب روان",
                    detailedNoteFa = "عرض جوب ۷۰ سانتی‌متر، خطر سقوط عصا و پا"
                ),
                DetectedObject(
                    id = "obj_metal_bridge",
                    labelFa = "پل فلزی عبور عابر",
                    labelEn = "Iron Bridge Footpath",
                    category = ObjectCategory.INFRASTRUCTURE,
                    boundingBox = RectF(0.55f, 0.62f, 0.78f, 0.92f),
                    confidence = 0.94f,
                    distanceMeters = 1.5f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "مسیر عبور امن در ساعت ۱",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 1,
                    colorFa = "آهنی زنگ‌زده و مشکی",
                    detailedNoteFa = "عرض پل ۶۰ سانتی‌متر، سطح فلزی مشبک"
                )
            )
        ),
        IranScenario(
            id = "yellow_bollards",
            titleFa = "میله‌های زرد مانع موتور و چاله پیاده‌رو",
            descriptionFa = "تشخیص موانع استوانه‌ای زرد رنگ شهرداری در پیاده‌روها و سنگفرش‌های برآمده",
            category = ObjectCategory.INFRASTRUCTURE,
            safetyStatus = SafetyStatus.CAUTION,
            situationAssessmentFa = "احتیاط! دو میله زرد فلزی در فاصله ۱.۸ متری ساعت ۱۱ و ۱۲. فاصله بین میله‌ها ۶۰ سانتی‌متر است.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_bollard_1",
                    labelFa = "میله زرد فلزی پیاده‌رو",
                    labelEn = "Yellow Pedestrian Bollard",
                    category = ObjectCategory.INFRASTRUCTURE,
                    boundingBox = RectF(0.38f, 0.40f, 0.49f, 0.88f),
                    confidence = 0.98f,
                    distanceMeters = 1.8f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "ایستا روبرو",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 12,
                    colorFa = "زرد ترافیکی با نوار شب‌رنگ",
                    detailedNoteFa = "ارتفاع ۸۰ سانتی‌متر، برخورد زانو یا دست"
                ),
                DetectedObject(
                    id = "obj_pothole_1",
                    labelFa = "چاله و سنگفرش خراب پیاده‌رو",
                    labelEn = "Pavement Pothole Hazard",
                    category = ObjectCategory.HAZARD,
                    boundingBox = RectF(0.15f, 0.72f, 0.35f, 0.92f),
                    confidence = 0.89f,
                    distanceMeters = 1.1f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "سمت چپ در کف زمین",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 10,
                    colorFa = "خاکی تیره",
                    detailedNoteFa = "عمق حدود ۱۰ سانتی‌متر"
                )
            )
        ),
        IranScenario(
            id = "iran_banknotes",
            titleFa = "شناسایی اسکناس‌های ایران (تومان)",
            descriptionFa = "اسکن و تشخیص خودکار مبلغ، رنگ و طرح اسکناس‌های رایج ایران با دقت بالا",
            category = ObjectCategory.CURRENCY,
            safetyStatus = SafetyStatus.SAFE,
            situationAssessmentFa = "ایران‌چک یکصد هزار تومانی (یک میلیون ریال) در دست شما، کاملاً واضح و با نور کافی.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_money_100k",
                    labelFa = "ایران‌چک ۱۰۰،۰۰۰ تومانی",
                    labelEn = "100,000 Tomans (1M Rials)",
                    category = ObjectCategory.CURRENCY,
                    boundingBox = RectF(0.15f, 0.25f, 0.85f, 0.75f),
                    confidence = 0.99f,
                    distanceMeters = 0.35f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "در دست کاربر",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 12,
                    colorFa = "آبی روشن با طرح میدان گنبد",
                    detailedNoteFa = "ارزش: صد هزار تومان (یک میلیون ریال). واترمارک و نخ امنیتی مشاهده شد."
                )
            )
        ),
        IranScenario(
            id = "iran_daily_home_medicine",
            titleFa = "دارو و وسایل روزمره منزل",
            descriptionFa = "تشخیص جعبه داروها، کلید، پریز برق، دستگیره در و لیوان چای داغ",
            category = ObjectCategory.DAILY_ITEM,
            safetyStatus = SafetyStatus.SAFE,
            situationAssessmentFa = "جعبه داروی استامینوفن کدئین روی میز در فاصله ۴۵ سانتی‌متری، لیوان چای داغ در سمت راست ساعت ۲.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_med_1",
                    labelFa = "جعبه قرص استامینوفن ۵۰۰",
                    labelEn = "Acetaminophen 500 Medicine Box",
                    category = ObjectCategory.DAILY_ITEM,
                    boundingBox = RectF(0.28f, 0.35f, 0.52f, 0.65f),
                    confidence = 0.96f,
                    distanceMeters = 0.45f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "روی میز مستقیم",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 12,
                    colorFa = "سفید و آبی با متن فارسی",
                    detailedNoteFa = "استامینوفن ۵۰۰ میلی‌گرم، ۱۰ عددی"
                ),
                DetectedObject(
                    id = "obj_hot_tea",
                    labelFa = "استکان چای داغ",
                    labelEn = "Hot Persian Tea Glass",
                    category = ObjectCategory.HAZARD,
                    boundingBox = RectF(0.62f, 0.40f, 0.82f, 0.72f),
                    confidence = 0.93f,
                    distanceMeters = 0.50f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "سمت راست ساعت ۲",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 2,
                    colorFa = "سرخ‌رنگ با بخار ملایم",
                    detailedNoteFa = "هشدار: مایع داغ، مراقب سوختگی دست باشید"
                )
            )
        ),
        IranScenario(
            id = "iran_crosswalk_taxi",
            titleFa = "خیابان، خط عابر و تاکسی زرد در حال عبور",
            descriptionFa = "تشخیص خط‌کشی عابر پیاده و تاکسی زرد پژو که با سرعت در حال نزدیک شدن است",
            category = ObjectCategory.VEHICLE,
            safetyStatus = SafetyStatus.CRITICAL,
            situationAssessmentFa = "خطر! تاکسی زرد با سرعت ۳۵ کیلومتر بر ساعت از سمت چپ (ساعت ۹) به خط عابر نزدیک می‌شود. فاصله ۵ متر. هنوز رد نشوید.",
            sampleObjects = listOf(
                DetectedObject(
                    id = "obj_yellow_taxi",
                    labelFa = "تاکسی زرد پژو ۴۰۵",
                    labelEn = "Yellow Iranian Taxi (Peugeot 405)",
                    category = ObjectCategory.VEHICLE,
                    boundingBox = RectF(0.08f, 0.28f, 0.55f, 0.72f),
                    confidence = 0.98f,
                    distanceMeters = 5.2f,
                    speedKmh = 35.0f,
                    velocityVector = Pair(0.40f, 0.15f),
                    trajectoryNameFa = "از چپ به راست به سمت خط عابر",
                    isApproaching = true,
                    isCollisionRisk = true,
                    timeToCollisionSeconds = 1.8f,
                    clockPosition = 10,
                    colorFa = "زرد خورشیدی با نوار شطرنجی مشکی",
                    detailedNoteFa = "سرعت بالا، حق تقدم با تاکسی است یا در حال ترمز نیست"
                ),
                DetectedObject(
                    id = "obj_crosswalk_stripes",
                    labelFa = "خط‌کشی عابر پیاده سفید",
                    labelEn = "Pedestrian Zebra Crosswalk",
                    category = ObjectCategory.INFRASTRUCTURE,
                    boundingBox = RectF(0.12f, 0.65f, 0.88f, 0.98f),
                    confidence = 0.96f,
                    distanceMeters = 1.0f,
                    speedKmh = 0f,
                    velocityVector = Pair(0f, 0f),
                    trajectoryNameFa = "مستقیم زیر پای شما",
                    isApproaching = false,
                    isCollisionRisk = false,
                    clockPosition = 12,
                    colorFa = "سفید شب‌نما روی آسفالت",
                    detailedNoteFa = "عرض خطوط ۵۰ سانتی‌متر"
                )
            )
        )
    )

    fun createSceneFromScenario(scenario: IranScenario): ScenePerception {
        return ScenePerception(
            objects = scenario.sampleObjects,
            ambientLight = "نور روز طبیعی",
            overallSafety = scenario.safetyStatus,
            immediateAudioAlertFa = scenario.situationAssessmentFa,
            fullAiDescription = "تحلیل چشم انسان: ${scenario.descriptionFa}. جزئیات دقیق: ${scenario.situationAssessmentFa}",
            fps = 60,
            inferenceLatencyMs = 12L
        )
    }
}
