package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

enum class VoiceEngineType(val titleFa: String) {
    ESPEAK_NG("موتور صوتی داخلی eSpeak-NG (فارسی، پرسرعت، آفلاین)"),
    ANDROID_SYSTEM("موتور صوتی سیستم اندروید")
}

class SpeechAndHapticEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    val espeakSynthesizer = EspeakNgVoiceSynthesizer()

    var activeEngineType: VoiceEngineType = VoiceEngineType.ESPEAK_NG
    var isPersianLanguage: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var sonarJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)
    private var isSonarMuted = true // Sonar beeper disabled by default per user request
    private var lastSpokenText: String = ""
    private var lastSpokenTime: Long = 0

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            val persianLocale = Locale.Builder().setLanguage("fa").setRegion("IR").build()
            val result = tts?.setLanguage(persianLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // If Persian is missing from device system TTS, eSpeak-NG is automatically favored!
                activeEngineType = VoiceEngineType.ESPEAK_NG
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(1.20f)
            tts?.setPitch(1.05f)
        }
    }

    /**
     * Speaks text immediately using either internal eSpeak-NG or Android TTS.
     * With eSpeak-NG, Persian phonemes are synthesized at maximum volume with 0ms latency.
     */
    fun speak(text: String, isUrgent: Boolean = false) {
        if (text.isBlank()) return

        val now = System.currentTimeMillis()
        if (!isUrgent && text == lastSpokenText && (now - lastSpokenTime) < 2200) {
            return
        }

        lastSpokenText = text
        lastSpokenTime = now

        if (activeEngineType == VoiceEngineType.ESPEAK_NG) {
            espeakSynthesizer.speak(text, isUrgent)
        } else {
            if (!isTtsReady) {
                // Fallback to eSpeak-NG if Android TTS is not ready
                espeakSynthesizer.speak(text, isUrgent)
                return
            }
            val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "didban_utterance_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        espeakSynthesizer.stop()
        tts?.stop()
    }

    fun setEngineType(type: VoiceEngineType) {
        activeEngineType = type
        val confirm = if (type == VoiceEngineType.ESPEAK_NG)
            "موتور صوتی داخلی فعال شد"
        else
            "موتور صوتی سیستم اندروید فعال شد"
        speak(confirm, isUrgent = true)
    }

    fun setVolumeMultiplier(multiplier: Float) {
        espeakSynthesizer.volumeMultiplier = multiplier
    }

    fun setLanguage(persian: Boolean) {
        isPersianLanguage = persian
        espeakSynthesizer.languageFa = persian
        val note = if (persian) "زبان فارسی انتخاب شد" else "English language selected"
        speak(note, isUrgent = true)
    }

    /**
     * Triggers tactile feedback corresponding to danger/proximity level.
     */
    fun triggerHapticFeedback(dangerLevel: String) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (dangerLevel) {
                    "CRITICAL" -> {
                        val timings = longArrayOf(0, 160, 70, 160, 70, 220)
                        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    "CAUTION" -> {
                        val timings = longArrayOf(0, 110, 60, 130)
                        val amplitudes = intArrayOf(0, 190, 0, 210)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    else -> {
                        vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (dangerLevel) {
                    "CRITICAL" -> vib.vibrate(longArrayOf(0, 160, 70, 160), -1)
                    "CAUTION" -> vib.vibrate(longArrayOf(0, 110, 60, 110), -1)
                    else -> vib.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Acoustic sonar radar beeps.
     */
    fun updateSonarRadar(nearestDistanceMeters: Float, hasObstacle: Boolean) {
        if (isSonarMuted || !hasObstacle || nearestDistanceMeters > 5.0f) {
            sonarJob?.cancel()
            sonarJob = null
            return
        }

        if (sonarJob?.isActive == true) return

        sonarJob = audioScope.launch {
            while (isActive && !isSonarMuted) {
                val delayMs = when {
                    nearestDistanceMeters <= 0.8f -> 120L
                    nearestDistanceMeters <= 1.5f -> 240L
                    nearestDistanceMeters <= 2.5f -> 480L
                    nearestDistanceMeters <= 3.5f -> 750L
                    else -> 1100L
                }
                val frequency = when {
                    nearestDistanceMeters <= 1.0f -> 1200
                    nearestDistanceMeters <= 2.0f -> 920
                    else -> 700
                }
                playShortBeep(frequency, 50)
                delay(delayMs)
            }
        }
    }

    fun stopSonar() {
        sonarJob?.cancel()
        sonarJob = null
    }

    fun toggleMuteSonar(muted: Boolean) {
        isSonarMuted = muted
        if (muted) stopSonar()
    }

    private fun playShortBeep(frequencyHz: Int, durationMs: Int) {
        try {
            val sampleRate = 16000
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val generatedSnd = ByteArray(2 * numSamples)
            var idx = 0
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz)
                val sample = (sin(angle) * 32767).toInt().toShort()
                generatedSnd[idx++] = (sample.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((sample.toInt() and 0xff00) ushr 8).toByte()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            audioScope.launch {
                delay(durationMs.toLong() + 20)
                audioTrack.release()
            }
        } catch (_: Exception) {}
    }

    fun shutdown() {
        stopSonar()
        espeakSynthesizer.stop()
        tts?.stop()
        tts?.shutdown()
    }
}
