package com.aricneto.twistytimer.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.aricneto.twistytimer.TwistyTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * A class used to create [CountDownTimer]s that vibrates and emits a tone once a specific
 * time has passed, depending on how it's built. Must be built using
 * [Builder].
 */
class CountdownWarning private constructor(builder: Builder) :
    CountDownTimer(builder.secondsInFuture * 1000, 50) {
    private val vibrator: Vibrator = TwistyTimer.getAppContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var toneGenerator: ToneGenerator? = null

    private val vibrateEnabled: Boolean
    private val vibrateDuration: Long

    private val toneEnabled: Boolean
    private val toneDuration: Int
    private val toneCode: Int

    init {

        this.vibrateEnabled = builder.vibrateEnabled
        this.vibrateDuration = builder.vibrateDuration

        this.toneEnabled = builder.toneEnabled
        this.toneDuration = builder.toneDuration
        this.toneCode = builder.toneCode
    }


    override fun onTick(l: Long) {
    }

    override fun onFinish() {
        if (vibrateEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        vibrateDuration,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrateDuration)
            }
        }
        if (toneEnabled) {
            try {
                this.toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator!!.startTone(toneCode, toneDuration)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(toneDuration.toLong().milliseconds)
                    if (toneGenerator != null) {
                        Log.d("Countdown", "toneGenerator released")
                        toneGenerator!!.release()
                        toneGenerator = null
                    }
                }
            } catch (e: Exception) {
                Log.d("Countdown", "Exception while playing sound:$e")
            }
        }
    }

    class Builder
    /**
     * Build a [CountdownWarning] object
     * 
     * @param secondsInFuture the countdown duration in seconds
     */(val secondsInFuture: Long) {
        var vibrateEnabled = true
        var vibrateDuration: Long = 300

        var toneEnabled = false
        var toneDuration = 300
        var toneCode = ToneGenerator.TONE_CDMA_PIP

        /**
         * If device should vibrate at the end of countdown
         * 
         * @param vibrateEnabled true if device should vibrate
         */
        fun withVibrate(vibrateEnabled: Boolean): Builder {
            this.vibrateEnabled = vibrateEnabled
            return this
        }

        /**
         * If device should emit a tone at the end of countdown
         * 
         * @param toneEnabled true if device should emit a tone
         */
        fun withTone(toneEnabled: Boolean): Builder {
            this.toneEnabled = toneEnabled
            return this
        }

        /**
         * Duration, in milliseconds of the vibration (if set)
         * 
         * @param vibrateDuration vibrate duration in milliseconds
         */
        fun vibrateDuration(vibrateDuration: Long): Builder {
            this.vibrateDuration = vibrateDuration
            return this
        }

        /**
         * Duration, in milliseconds of the tone (if set)
         * 
         * @param toneDuration tone duration in milliseconds
         */
        fun toneDuration(toneDuration: Int): Builder {
            this.toneDuration = toneDuration
            return this
        }

        /**
         * Code for the tone that should play (if set)
         * Must be one of [ToneGenerator]s tone constants
         * 
         * @param toneCode the tone code, a [ToneGenerator] constant
         */
        fun toneCode(toneCode: Int): Builder {
            this.toneCode = toneCode
            return this
        }

        fun build(): CountdownWarning {
            return CountdownWarning(this)
        }
    }
}
