package com.aricneto.twistytimer.stats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.loader.content.AsyncTaskLoader
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.TTIntent.ACTION_STATISTICS_LOADED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MOVED_TO_HISTORY
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED
import com.aricneto.twistytimer.utils.TTIntent.BroadcastBuilder
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.getSolve
import com.aricneto.twistytimer.utils.Wrapper
import com.aricneto.twistytimer.utils.Wrapper.Companion.wrap
import kotlinx.coroutines.runBlocking

/**
 * 
 * 
 * A loader used to populate a [Statistics] object from the database for use in the timer
 * and timer graph (statistics table) fragments. The main timer fragment will manage the listener
 * and notify its subordinate fragments of any updates.
 */
class StatisticsLoader(
    context: Context, statistics: Statistics,
    puzzleType: String?, puzzleSubtype: String?, mode: Int
) : AsyncTaskLoader<Wrapper<Statistics?>?>(context) {
    /**
     * The cached statistics that have been loaded previously. This reference will be reset to
     * `null` if the data changes and the data will need to be loaded again when requested.
     */
    private val mStatistics: Statistics

    /**
     * A wrapper around the loaded statistics. This is required to allow the same `Statistics`
     * instance to be used for every load while ensuring that the `LoaderManager` sees a
     * different object delivered, otherwise it would not invoke `onLoadFinished` on the
     * activity or fragment waiting for the updated data.
     */
    private var mLoadedData: Wrapper<Statistics?>

    /**
     * The puzzle type. This is fixed for the lifetime of the loader instance.
     */
    private val mPuzzleType: String?

    /**
     * The puzzle subtype. This is fixed for the lifetime of the loader instance.
     */
    private val mPuzzleSubtype: String?

    private val mMode: Int

    /**
     * The broadcast receiver that is notified of changes to the solve time data.
     */
    private var mTimeDataChangedReceiver: BroadcastReceiver? = null

    /**
     * A broadcast receiver that is notified of changes to the solve time data.
     */
    private class TimeDataChangedReceiver(
        /**
         * The loader to be notified of changes to the solve time data.
         */
        private val mLoader: StatisticsLoader
    ) : BroadcastReceiver() {
        /**
         * Receives notification of a change to the solve time data and notifies the loader if the
         * change is pertinent and a new load is required.
         * 
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that has occurred.
         */
        override fun onReceive(context: Context?, intent: Intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent)
            when (intent.getAction()) {
                ACTION_TIME_ADDED -> if (!mLoader.deliverQuickResult(intent)) {
                    if (DEBUG_ME) Log.d(TAG, "  Quick update not possible. Will reload!")
                    mLoader.onContentChanged()
                } // else updated statistics have been delivered without a full re-load.

                ACTION_TIMES_MODIFIED, ACTION_TIMES_MOVED_TO_HISTORY -> {
                    if (DEBUG_ME) Log.d(TAG, "  Unknown changes or history toggle. Will reload!")
                    mLoader.onContentChanged()
                }
            }
        }
    }

    /**
     * Creates a new loader for the solve time statistics. The given statistics define the set of
     * "average-of-N" calculations to be made.
     * 
     * @param context
     * @param statistics
     * @param puzzleType
     * @param puzzleSubtype
     * @param mode
     */
    init {
        if (DEBUG_ME) Log.d(TAG, "Created new Loader for Statistics!")

        mStatistics = statistics
        mPuzzleType = puzzleType
        mPuzzleSubtype = puzzleSubtype
        mMode = mode

        mStatistics.reset()
        mLoadedData = wrap<Statistics?>(null)
    }

    /**
     * Attempts a quick update of the statistics without resorting to a full read of the database.
     * If the statistics were previously read from the database, then a single new time, added for
     * the current session, can be added directly to the statistics and the update can be delivered
     * to the activity or fragment.
     * 
     * @param intent
     * The intent that may contain details of a new solve time.
     * 
     * @return
     * `true` if the statistics were up-to-date with respect to the database and the
     * intent contained a new solve time that was added to the statistics directly, avoiding
     * the need for a full database re-load; or `false` if a full database reload will
     * still be required to update the statistics.
     */
    private fun deliverQuickResult(intent: Intent): Boolean {
        if (!mLoadedData.isEmpty()) {
            // All statistics were loaded previously from the database (because the wrapper is not
            // empty), so try a quick update.
            val solve = getSolve(intent)

            if (solve != null && solve.mode == mMode) {
                if (solve.penalty == PuzzleUtils.PENALTY_DNF) {
                    mStatistics.addDNF(true)
                } else {
                    mStatistics.addTime(solve.time.toLong(), true)
                }

                mLoadedData = mLoadedData.rewrap() // See explanation in "loadInBackground".

                if (DEBUG_ME) Log.d(TAG, "  Delivering quick update to statistics!")
                deliverResult(mLoadedData) // Will trigger "onLoadFinished" in Fragment/Activity.

                return true
            }
        }

        return false
    }

    /**
     * Starts loading the statistics from the database. If statistics were previously loaded, they
     * will be re-delivered. If the statistics that were so delivered are out of date, or if no
     * statistics were available for immediate delivery, a new full re-load of the statistics from
     * the database will be triggered and deliver will occur once that background task completes.
     */
    override fun onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading")

        if (!mLoadedData.isEmpty()) {
            // If statistics are available, deliver them now (even if they are not up-to-date).
            if (DEBUG_ME) Log.d(TAG, "  Delivering available Statistics...")
            deliverResult(mLoadedData)
        }

        // If not already listening for changes to the time data, start listening now. If any
        // pertinent change is detected (i.e., one that would impact on the validity of the
        // statistics), the statistics will need to be updated or reloaded.
        if (mTimeDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG, "  Starting monitoring of changes affecting Statistics.")
            mTimeDataChangedReceiver = TimeDataChangedReceiver(this)
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(
                mTimeDataChangedReceiver!!, TTIntent.CATEGORY_TIME_DATA_CHANGES
            )
        }

        // If any pertinent change was detected by the receiver, or if no statistics have been
        // loaded, then perform a full load from the database now.
        if (takeContentChanged() || mLoadedData.isEmpty()) {
            if (DEBUG_ME) Log.d(TAG, "  forceLoad() called...")
            forceLoad()
            commitContentChanged()
        }
    }

    override fun onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset")

        super.onReset()

        // "Unload" any previously loaded statistics, so a full re-load will happen the next time.
        mLoadedData = wrap<Statistics?>(null) // "null" flags "not loaded" state.
        mStatistics.reset()

        if (mTimeDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG, "  Stopping monitoring of changes affecting Statistics.")
            // Receiver will be re-registered in "onStartLoading", if that is called again.
            TTIntent.unregisterReceiver(mTimeDataChangedReceiver!!)
            mTimeDataChangedReceiver = null
        }
    }

    /**
     * Loads the statistics from the database on a background thread.
     * 
     * @return The loaded statistics.
     */
    override fun loadInBackground(): Wrapper<Statistics?>? {
        var startTime// For when "DEBUG_ME" is false.
                = 0L
        if (DEBUG_ME) {
            Log.d(TAG, "loadInBackground")
            startTime = SystemClock.elapsedRealtime()
        }

        // This is a full, clean load, so clear out the results from the previous load.
        mStatistics.reset()

        // TODO: Add support for cancellation: add a call-back to "populateStatistics", so it can
        // poll the cancellation status as it iterates over the solves it reads from the database.
        runBlocking<Unit> {
            TwistyTimer.getSolveRepository().populateStatistics(mPuzzleType!!, mPuzzleSubtype!!, mMode, mStatistics)
        }

        if (DEBUG_ME) {
            Log.d(
                TAG, String.format(
                    "  Loaded Statistics in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime
                )
            )
            BroadcastBuilder(CATEGORY_TIME_DATA_CHANGES, ACTION_STATISTICS_LOADED)
                .longValue(SystemClock.elapsedRealtime() - startTime)
                .broadcast()
        }

        // If this is not the first time loading the data, a different object must be returned if
        // the "LoaderManager" is to trigger "onLoadFinished" (go figure). As "mStatistics" is
        // still the same object, a new wrapper around that object is created instead to trick
        // "LoaderManager" into doing what is expected.
        return wrap<Statistics?>(mStatistics).also {
            mLoadedData = it
        } // Old content may have been null.
    }

    companion object {
        /**
         * Flag to enable debug logging from this class.
         */
        private const val DEBUG_ME = true

        /**
         * A "tag" used to identify this class as the source of log messages.
         */
        private val TAG: String = StatisticsLoader::class.java.getSimpleName()
    }
}
