package com.aricneto.twistytimer.layout

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.text.Html
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.PuzzleUtils.FORMAT_NO_MILLI
import com.aricneto.twistytimer.utils.PuzzleUtils.FORMAT_SMALL_MILLI
import com.aricneto.twistytimer.utils.PuzzleUtils.NO_PENALTY
import com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_DNF
import com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_PLUSTWO
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString

/*
* The Android chronometer widget revised to count milliseconds
*/

/**
 * A chronometer for twisty puzzles of all types. This supports timing in milliseconds. Display of
 * the elapsed time to a high resolution (hundredths of a second) or low resolution (whole seconds),
 * addition of standard "+2" and "DNF" penalties, and "hold-for-start" behavior that can restore a
 * previous time if the hold is canceled.
 */
class ChronometerMilli @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {
    private var hideTimeText: String? = null
    private var hideTimeEnabled = false

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was started.
     * Will be zero if the chronometer has not been started or has been reset.
     */
    private var mStartedAt: Long = 0

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was stopped.
     * Will be zero if the chronometer has not been stopped or has been reset.
     */
    private var mStoppedAt: Long = 0

    /**
     * The code for additional penalty. Values from PuzzleUtils are supported.
     */
    private var mPenalty = 0

    private var mIsVisible = false

    /**
     * Indicates if this chronometer has been started. If started, it can be stopped, but it cannot
     * be started again or reset until it has been stopped. This should be tested before calling
     * [.start], [.stop] or [.reset], if the state is not already known.
     * 
     * @return
     * `true` if this chronometer has been started; or `false` if it has not been
     * started.
     */
    var isStarted: Boolean = false
        private set
    private var mIsRunning = false

    /**
     * Indicates if this chronometer is holding in readiness to be started once the minimum hold
     * period has elapsed.
     */
    private var mIsHoldingForStart = false

    /**
     * The text that was being displayed by this chronometer before entering the hold-for-start
     * state. If the state is canceled, this text will be restored.
     */
    private var mTextSavedBeforeHolding: CharSequence? = null

    /**
     * Indicates if seconds will be shown to a high resolution while the timer is started. If
     * enabled, fractions (hundredths) of a second will be displayed while the chronometer is
     * running. See [.updateText] for details on how and when this preference is applied.
     */
    private var mShowHiRes = false

    /**
     * The normal text color. This is saved before the text is highlighted and restored when
     * highlighting is turned off.
     */
    private var mNormalColor = 0

    private fun init() {
        // Save the current (normal) text color, as it may be overwritten with the highlight color.
        // The highlight color is available from "getHighlightColor", as that is never changed.
        // The implementation assumes that different colors for different states will not be used.
        mNormalColor = currentTextColor

        mShowHiRes = getBoolean(R.string.pk_show_hi_res_timer, true)
        hideTimeEnabled = getBoolean(R.string.pk_hide_time_while_running, false)
        hideTimeText = context.getString(R.string.hideTimeText)

        // The initial state will cause "0.00" to be displayed.
        updateText()
    }

    /**
     * Sets the highlighted state of the time value text displayed by this chronometer. This can be
     * used with the start cue and hold-for-start behavior.
     * 
     * @param isHighlighted
     * `true` to highlight the text in a different color; or `false` to restore the
     * normal text color.
     */
    fun setHighlighted(isHighlighted: Boolean) {
        setTextColor(if (isHighlighted) highlightColor else mNormalColor)
    }

    val elapsedTime: Long
        /**
         * Gets the elapsed time (in milliseconds) measured by this chronometer including any additional
         * time penalty. This method may be called even if this chronometer is currently started. Any
         * penalty time set by [.setPenalty] will be included in the reported elapsed time.
         * If a "DNF" penalty was applied, the elapsed time will be reported as zero.
         * 
         * @return
         * The elapsed time measured by this chronometer including any time penalty, or zero if
         * the penalty is a "DNF".
         */
        get() {
            return when (mPenalty) {
                PENALTY_DNF -> 0L

                PENALTY_PLUSTWO -> this.elapsedTimeExcludingPenalties + TWO_SECOND_PENALTY_MS

                else -> this.elapsedTimeExcludingPenalties
            }
        }

    private val elapsedTimeExcludingPenalties: Long
        /**
         * Gets the elapsed time (in milliseconds) measured by this chronometer excluding any additional
         * time penalties. This method may be called even if this chronometer is currently started. Any
         * penalty time set by [.setPenalty] will *not* be included in the reported
         * elapsed time.
         * 
         * @return The elapsed time measured by this chronometer excluding penalties.
         */
        get() =// If the chronometer is started, then the elapsed time is the difference between "now" and
        // "mStartedAt". If the chronometer has never been started, has been stopped, or has been
        // reset, then the difference between "mStoppedAt" and "mStartedAt" is used. This ensures
            // that the initial state or reset state will display "0.00".
            (if (this.isStarted) SystemClock.elapsedRealtime() else mStoppedAt) - mStartedAt

    /**
     * Holds the chronometer is a state ready to be started from zero. This will display a zero
     * start time, but, if [.cancelHoldForStart] is called, the previously displayed value
     * be restored. If [.start] is called subsequently, the recorded elapsed time and any
     * penalties will *not* be reset automatically, so be sure to call [.reset] first,
     * if appropriate. Both of those methods also exit this state, so `cancelHoldForStart()`
     * will no longer have any effect.
     * 
     * @throws IllegalStateException
     * If the chronometer is already started.
     */
    fun holdForStart() {
        check(!this.isStarted) { "Cannot hold chronometer if already started." }

        // "TimerFragment" directly sets the text on this chronometer view when doing an inspection
        // count-down, inspection penalty, or displaying "DNF". Those functions should really be
        // performed using a separate text view, or should be properly integrated into this class.
        // In the meantime, before holding for the start, save the displayed text (whatever it is)
        // and restore it if "cancelHoldForStart" is called. Do not call "updateText" to restore the
        // previous elapsed time, as "TimerFragment" may have hijacked this view to show something
        // else.
        //
        // Also, this "where-did-that-text-come-from?" condition can also be the result of the
        // default state-saving of this view, as full state saving and restoration of the elapsed
        // time, penalties, etc. is not yet implemented.
        mIsHoldingForStart = true
        mTextSavedBeforeHolding = getText()
        updateText() // Will display "0.00" because "mIsHoldingForStart" is set.
    }

    /**
     * Cancels the hold-for-start state and restores the value previously displayed by this
     * chronometer. If the chronometer is not in the hold-for-start state, this method will have
     * no effect.
     */
    fun cancelHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false
            if (mTextSavedBeforeHolding != null) {
                // Do not call "updateText" to restore the saved value, as the saved text may not
                // have been set by this chronometer.
                text = mTextSavedBeforeHolding
            }
        }
    }

    /**
     * Ends the hold-for-start state *without* restoring the value previously displayed by
     * this chronometer. If the chronometer is not in the hold-for-start state, this method will
     * have no effect. This method is called automatically if the chronometer is started or reset.
     */
    private fun endHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false
            mTextSavedBeforeHolding = null
        }
    }

    /**
     * Starts the chronometer, resuming the recording of the elapsed time from where it left off
     * when it was last stopped. To restart from zero and clear penalties, call [.reset]
     * first. If this chronometer is already started, calling this method will have no effect.
     * This will also exit the "hold-for-start" state if it is active; the displayed text value
     * saved when that state was entered will not be restored.
     */
    fun start() {
        if (this.isStarted) {
            return
        }

        // For some puzzle types, the elapsed time could be long (many minutes, or even hours), so
        // the need to support a "pause" feature during informal timing sessions may be useful.
        // Here, calculate the new "mStartedAt" value and then offset it into the past by the
        // amount of elapsed time already recorded, which will allow sequences of state changes
        // such as "reset-start-stop-start-stop-start-stop" to accumulate time as necessary. If
        // already started, "stop-start" is effectively "pause-resume". Do not include any penalty
        // time in the elapsed time offset, it will remain separate.
        mStartedAt = SystemClock.elapsedRealtime() - this.elapsedTimeExcludingPenalties
        mStoppedAt = 0L
        this.isStarted = true

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart()

        updateText()
        updateRunning()
    }

    /**
     * Stops the chronometer. The elapsed time will no longer be incremented until the chronometer
     * is started again. If this chronometer is already stopped, calling this method will have no
     * effect.
     * 
     * @throws IllegalStateException
     * If the chronometer is already started.
     */
    fun stop() {
        if (!this.isStarted) {
            return
        }

        this.isStarted = false
        mStoppedAt = SystemClock.elapsedRealtime()

        // Update the text to show the exact elapsed time at this precise moment.
        updateText()

        // Stop updating the display if necessary, as the chronometer is no longer running.
        updateRunning()
    }

    /**
     * Resets the time to zero. The chronometer must be stopped before it can be reset. This will
     * also exit the "hold-for-start" state if it is active; the displayed text value saved when
     * that state was entered will not be restored.
     * 
     * @throws IllegalStateException
     * If the chronometer is currently started.
     */
    @Throws(IllegalStateException::class)
    fun reset() {
        check(!this.isStarted) { "Chronometer cannot be reset if it has been started." }

        mStartedAt = 0L
        mStoppedAt = 0L
        mPenalty = NO_PENALTY

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart()

        // No need to call "updateRunning()", as we have not changed the "running" state.
        updateText()
    }

    /**
     * Sets a penalty to be applied to the currently recorded elapsed time. If a 2-second penalty
     * is applied, a "+" is appended to the display of the elapsed time to indicate that a penalty
     * time has been added and [.getElapsedTime] will include the extra penalty. If a
     * did-not-finish penalty is set, "DNF" is displayed. Any previously set penalty is replaced
     * by the new penalty. The `NO_PENALTY` value can also be set to remove a 2-second or
     * DNF penalty and restore the elapsed time.
     * 
     * @param penalty
     * The code for the penalty to be applied. Use only [NO_PENALTY],
     * [PENALTY_PLUSTWO] or [PENALTY_DNF].
     * 
     * @throws IllegalArgumentException
     * If the penalty code is not one of those supported by this method.
     */
    fun setPenalty(penalty: Int) {
        when (penalty) {
            NO_PENALTY, PENALTY_PLUSTWO, PENALTY_DNF -> mPenalty = penalty
            else -> throw IllegalArgumentException("Penalty code is not allowed.")
        }

        // Show the new time with the included penalty and the "+" penalty indicator, if needed.
        updateText()
    }

    /**
     * 
     * 
     * Updates the text that displays the current elapsed time. The formatting of the time depends
     * on the state of the chronometer and the preference for showing fractional seconds values.
     * When the chronometer is stopped, fractional seconds values are shown. When the chronometer is
     * started (running), fractional seconds are only shown if the respective preference is enabled.
     * Fractional seconds are never shown for elapsed times of one hour or longer, regardless of the
     * state of the chronometer.
     * 
     * 
     * 
     * A preference to hide the elapsed time while the chronometer is running is also supported. If
     * the preference is enabled and the chronometer is started, then the elapsed time will not be
     * shown; a fixed string will be shown in its place.
     * 
     * 
     * 
     * If a "+2" penalty has been applied and the chronometer is stopped, "+" will be appended to
     * the display of the elapsed time. If a "DNF" penalty has been applied, "DNF" will be displayed
     * instead of the elapsed time.
     * 
     * 
     * @return
     * `true` if the displayed text presented a high-resolution, fractional value for
     * the number of seconds, or `false` if only whole seconds were shown. This may be
     * used to inform the necessary update frequency.
     */
    @Synchronized
    private fun updateText(): Boolean {
        // The displayed elapsed time will include any time penalty. If holding before starting,
        // then assume that the elapsed time will be started at zero and ignore the previously
        // recorded elapsed time and any current penalty.
        var timeText: String?
        val isHiRes: Boolean

        if (this.isStarted && hideTimeEnabled) {
            timeText = hideTimeText
            isHiRes = false
        } else if (!mIsHoldingForStart && mPenalty == PENALTY_DNF) {
            timeText = "DNF"
            isHiRes = false
        } else {
            val elapsedMS = if (mIsHoldingForStart) 0L else this.elapsedTime
            val hours = elapsedMS / (3600000L)

            isHiRes = (!this.isStarted || mShowHiRes) && hours == 0L

            timeText = if (elapsedMS > 0) convertTimeToString(elapsedMS, if (isHiRes) FORMAT_SMALL_MILLI else FORMAT_NO_MILLI)
            else "0<small>.00</small>"
            // If a "+2" penalty has been applied and the chronometer is not started or holding,
            // append a small "+" to the time text to declare that a penalty has been added.
            if (!this.isStarted && !mIsHoldingForStart && mPenalty == PENALTY_PLUSTWO) {
                timeText += " <small>+</small>"
            }
        }

        text = Html.fromHtml(timeText, Html.FROM_HTML_MODE_LEGACY)

        return isHiRes
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsVisible = false
        updateRunning()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        mIsVisible = visibility == VISIBLE
        updateRunning()
    }

    /**
     * Updates the running state of this chronometer. The chronometer is "running" if it is in the
     * started state and is visible. In the "running" state, the display of the current elapsed time
     * will be updated regularly.
     */
    private fun updateRunning() {
        val running = mIsVisible && this.isStarted

        if (running != mIsRunning) {
            // State has changed:
            //
            //   If the chronometer was not running but has now started running, then kick off a
            //   chain of messages that will update the display of the elapsed time at regular
            //   intervals. One message is queued here and then a new message is queued as each
            //   message is handled by "TimeUpdateHandler.handleMessage".
            //
            //   If the chronometer was running but has now stopped running, clear "mIsRunning"
            //   (which causes "TimeUpdateHandler.handleMessage" to break the chain of update
            //   messages) and then clear any other unhandled "tick" messages from the queue.
            //
            // If the state has not changed, then things can be left alone: either the message
            // chain is active and perpetuating itself, or it is inactive.
            mIsRunning = running

            if (mIsRunning) {
                // Use a very short "tick" time (1 ms) before the very first update.
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT, this), 1L)
            } else {
                mHandler.removeMessages(TICK_WHAT)
            }
        }
    }

    private val mHandler: Handler = TimeUpdateHandler()

    init {
        init()
    }

    // "static" handler class to prevent memory leaks.
    private class TimeUpdateHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(m: Message) {
            if (m.obj != null) {
                val chronometer = m.obj as ChronometerMilli

                // Update the time display before checking if the chronometer is still "running".
                // This ensures that the time display is up-to-date with the exact elapsed time.
                //
                // Adapt the interval between updates to the current resolution of the display
                // of the seconds value, i.e., update faster if showing 100ths of a second.
                val tickTime: Long = if (chronometer.updateText()) TICK_TIME_HR else TICK_TIME_LR

                if (chronometer.mIsRunning) {
                    // Only chain a new message for the next update if still running.
                    sendMessageDelayed(Message.obtain(this, TICK_WHAT, chronometer), tickTime)
                }
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "Chronometer"

        /**
         * The penalty time in milliseconds for a standard "+2" penalty.
         */
        private const val TWO_SECOND_PENALTY_MS = 2000L

        /**
         * The number of milliseconds between updates to the display of a low-resolution elapsed time.
         */
        private const val TICK_TIME_LR = 100L // 0.1 seconds to avoid jerkiness.

        /**
         * The number of milliseconds between updates to the display of a high-resolution elapsed
         * time.
         */
        private const val TICK_TIME_HR = 30L // 0.03 seconds (33 fps). 3 times fewer updates

        // than 0.01 seconds, but no noticeable visual difference.
        private const val TICK_WHAT = 2
    }
}
