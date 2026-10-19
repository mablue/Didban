package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiRawApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): okhttp3.ResponseBody
}

class GeminiVisionService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .build()

    private val api: GeminiRawApi = retrofit.create(GeminiRawApi::class.java)

    /**
     * Converts Bitmap to JPEG base64 string.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Deep Scene Perception: reports everything a human eye perceives in Persian.
     */
    suspend fun analyzeFullHumanScene(
        bitmap: Bitmap?,
        contextPrompt: String = "یک انسان نابینا در حال حرکت است. تمام آنچه از چشم می‌بینی، فواصل اجسام، مسیر حرکت، موانع شهری در ایران و اشیاء را بسیار رسا، خلاصه و کاربردی گزارش کن."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "کلید هوش مصنوعی Gemini تنظیم نشده است. سیستم بر روی پردازش آفلاین YOLO و سناریوهای بومی ایران فعال است."
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            // Text part
            val textPart = JSONObject().apply {
                put("text", contextPrompt)
            }
            parts.put(textPart)

            // Image part if available
            bitmap?.let {
                val imageBase64 = bitmapToBase64(it)
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", imageBase64)
                }
                val imgPart = JSONObject().apply {
                    put("inlineData", inlineData)
                }
                parts.put(imgPart)
            }

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            // System instruction
            val sysInstruction = JSONObject().apply {
                val sysParts = JSONArray()
                sysParts.put(JSONObject().apply {
                    put("text", "شما دیدبان هوشمند نابینایان در ایران هستید. دقیق، سریع، با تمرکز بر امنیت جانی، تشخیص موانع (جوب، موتور در پیاده‌رو، پل فلزی، میله زرد)، اسکناس‌های تومان و خطرات با گفتار فارسی شیوا پاسخ دهید.")
                })
                put("parts", sysParts)
            }
            root.put("systemInstruction", sysInstruction)

            val body = root.toString().toRequestBody("application/json".toMediaType())
            val responseBody = api.generateContent(apiKey, body)
            val jsonString = responseBody.string()
            val json = JSONObject(jsonString)

            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val candidateContent = candidate?.optJSONObject("content")
            val candidateParts = candidateContent?.optJSONArray("parts")
            val text = candidateParts?.optJSONObject(0)?.optString("text")

            text ?: "پاسخی از مدل دریافت نشد."
        } catch (e: Exception) {
            "خطا در ارتباط با سرور دیدبان: ${e.localizedMessage ?: "عدم دسترسی به اینترنت"}"
        }
    }
}
