package com.aricneto.twistytimer.stats

import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs.getDefaultIntValue
import com.aricneto.twistytimer.utils.Prefs.getInt
import java.util.TreeMap
import java.util.TreeSet


/**
 * A collection of [AverageCalculator] instances that distributes solve times to each
 * calculator. The calculators can be segregated into averages for times from the current session
 * only, and averages for times from all past sessions (including the current session). This also
 * provides a simple API to access the all-time and session mean, best and worst times and solve
 * count.
 * 
 * @author damo
 */
class Statistics
/**
 * Creates a new collection for statistics. Use a factory method to create a standard set of
 * statistics.
 */
private constructor() {
    /**
     * The average calculators for averages of times across all sessions. The calculators are keyed
     * by the number of times used to calculate the average.
     */
    private val mAllTimeACs: MutableMap<Int, AverageCalculator> =
        HashMap<Int, AverageCalculator>()

    /**
     * The average calculators for averages of times in the current session only. The calculators
     * are keyed by the number of times used to calculate the average.
     */
    private val mSessionACs: MutableMap<Int, AverageCalculator> =
        HashMap<Int, AverageCalculator>()

    /**
     * The frequencies of solve times across all sessions. The keys are the solve times in
     * milliseconds, but truncated to whole seconds, and the values are the number of solve times.
     * [AverageCalculator.DNF] may also be a key.
     */
    // NOTE: A "TreeMap" ensures that the entries are ordered by key (time) value.
    private val mAllTimeTimeFreqs = TreeMap<Long, Int>()

    /**
     * The frequencies of solve times for the current session. The keys are the solve times in
     * milliseconds, but truncated to whole seconds and the values are the number of solve times.
     * [AverageCalculator.DNF] may also be a key.
     */
    private val mSessionTimeFreqs = TreeMap<Long, Int>()

    /**
     * An average calculator for solves across all past sessions and the current session. May be
     * `null`.
     */
    private var mOneAllTimeAC: AverageCalculator? = null

    /**
     * An average calculator for solves only in the current session. May be `null`.
     */
    private var mOneSessionAC: AverageCalculator? = null

    /**
     * Resets all statistics and averages that have been collected previously. The average-of-N
     * calculators and time frequencies are reset, but the average-of-N calculators are not removed.
     */
    fun reset() {
        for (allTimeAC in mAllTimeACs.values) {
            allTimeAC.reset()
        }

        for (sessionAC in mSessionACs.values) {
            sessionAC.reset()
        }

        mAllTimeTimeFreqs.clear()
        mSessionTimeFreqs.clear()
    }

    val isForCurrentSessionOnly: Boolean
        /**
         * Indicates if all of the solve time averages required are across the current session only. If
         * only times for the current session are required, a more efficient approach may be taken to
         * load the saved solve times.
         * 
         * @return
         * `true` if all required averages apply only to solve times for the current session;
         * or `false` if the averages include at least one average for solve times across all
         * past and current sessions. If no averages for either set of solve times are required,
         * the result will be `true`.
         */
        get() = mOneAllTimeAC == null

    val nsOfAverages: IntArray
        /**
         * Gets an array of all distinct values of "N" for which calculators for the "average-of-N"
         * have been added to these statistics. There is no distinction between current-session-only
         * and all-time average calculators; the distinct set of values of "N" across both sets of
         * calculators is returned.
         * 
         * @return
         * The distinction values of "N" for all "average-of-N" calculators in these statistics.
         */
        get() {
            // NOTE: This is intended only to support the needs of "ChartStatistics", which assumes
            // that the union of all average calculators (created by appropriate factory methods in
            // this "Statistics" class) are exclusively for the current session or exclusively for all
            // sessions, not a mixture of both. "ChartStatistics" just needs to get all values of "N"
            // for which averages are required, so that it can create corresponding objects to store
            // the chart data. This ensures that it is not coupled to the details of the factory
            // methods in this class. An efficient implementation is not of much concern here.
            val distinctNs: MutableSet<Int> = TreeSet<Int>()

            distinctNs.addAll(mAllTimeACs.keys)
            distinctNs.addAll(mSessionACs.keys)

            val ns = IntArray(distinctNs.size)
            var i = 0

            for (n in distinctNs) {
                ns[i++] = n
            }

            return ns
        }

    /**
     * Creates a new calculator for the "average of *n*" solve times.
     * 
     * @param n
     * The number of solve times that will be averaged (e.g., 3, 5, 12, ...). Must be greater
     * than zero. If a calculator for the same value of `n` has been added for the same
     * value of `isForCurrentSessionOnly`, the previous calculator will be overwritten.
     * @param isForCurrentSessionOnly
     * `true` to collect times only for the current session, or `false` to collect
     * times across all past and current sessions.
     * 
     * @throws IllegalArgumentException
     * If `n` is not greater than zero.
     */
    private fun addAverageOf(n: Int, trimPercent: Int, isForCurrentSessionOnly: Boolean) {
        val ac = AverageCalculator(n, trimPercent)

        if (isForCurrentSessionOnly) {
            mSessionACs.put(n, ac)
            if (mOneSessionAC == null) {
                mOneSessionAC = ac
            }
        } else {
            mAllTimeACs.put(n, ac)
            if (mOneAllTimeAC == null) {
                mOneAllTimeAC = ac
            }
        }
    }

    /**
     * Gets the calculator for the "average of *n*" solve times.
     * 
     * @param n
     * The number of solve times that were averaged (e.g., 3, 5, 12, ...).
     * @param isForCurrentSessionOnly
     * `true` for the calculator that collected times only for the current session, or
     * `false` to for the calculator that collected times across all past and current
     * sessions.
     * 
     * @return
     * The requested average calculator, or `null` if no such calculator was defined for
     * these statistics.
     */
    fun getAverageOf(n: Int, isForCurrentSessionOnly: Boolean): AverageCalculator? {
        if (isForCurrentSessionOnly) {
            return mSessionACs.get(n)
        }
        return mAllTimeACs.get(n)
    }

    /**
     * Records a solve time. The time value should be in milliseconds. If the solve is a DNF,
     * call [.addDNF] instead.
     * 
     * @param time
     * The solve time in milliseconds. Must be positive (though [AverageCalculator.DNF]
     * is also accepted).
     * @param isForCurrentSession
     * `true` if the solve was added during the current session; or `false` if
     * the solve was added in a previous session.
     * 
     * @throws IllegalArgumentException
     * If the time is not greater than zero and is not `DNF`.
     */
    @Throws(IllegalArgumentException::class)
    fun addTime(time: Long, isForCurrentSession: Boolean) {
        // "time" is validated on the first call to "AverageCalculator.addTime".
        for (allTimeAC in mAllTimeACs.values) {
            allTimeAC.addTime(time)
        }

        if (isForCurrentSession) {
            for (sessionAC in mSessionACs.values) {
                sessionAC.addTime(time)
            }
        }

        // Updated the time frequencies.
        val timeForFreq =
            if (time == AverageCalculator.DNF) AverageCalculator.DNF else (time - time % 1000)
        var oldFreq: Int?

        oldFreq = mAllTimeTimeFreqs.get(timeForFreq)
        mAllTimeTimeFreqs.put(timeForFreq, if (oldFreq == null) 1 else oldFreq + 1)

        if (isForCurrentSession) {
            oldFreq = mSessionTimeFreqs.get(timeForFreq)
            mSessionTimeFreqs.put(timeForFreq, if (oldFreq == null) 1 else oldFreq + 1)
        }
    }

    /**
     * Records a did-not-finish (DNF) solve, one where no time was recorded.
     * 
     * @param isForCurrentSession
     * `true` if the DNF solve was added during the current session; or `false` if
     * the solve was added in a previous session.
     */
    // This methods takes away any confusion about what time value represents a DNF.
    fun addDNF(isForCurrentSession: Boolean) {
        addTime(AverageCalculator.DNF, isForCurrentSession)
    }

    val sessionBestTime: Long
        /**
         * Gets the best solve time of all those added to these statistics for a solve in the current
         * session.
         * 
         * @return
         * The best time ever added for the current session. The result will be
         * [AverageCalculator.UNKNOWN] if no times have been added, or if all added times
         * were DNFs.
         */
        get() = mOneSessionAC?.bestTime ?: AverageCalculator.UNKNOWN

    val sessionWorstTime: Long
        /**
         * Gets the worst time (not a DNF) of all those added to these statistics for a solve in the
         * current session.
         * 
         * @return
         * The worst time ever added for the current session. The result will be
         * [AverageCalculator.UNKNOWN] if no times have been added, or if all added times
         * were DNFs.
         */
        get() = mOneSessionAC?.worstTime ?: AverageCalculator.UNKNOWN

    val sessionNumSolves: Int
        /**
         * Gets the total number of solve times (including DNFs) that were added to these statistics
         * for the current session. To get the number of non-DNF solves, subtract the result of
         * [.getSessionNumDNFSolves].
         * 
         * @return The number of solve times that were added for the current session.
         */
        get() = mOneSessionAC?.numSolves ?: 0

    val sessionNumDNFSolves: Int
        /**
         * Gets the total number of DNF solves that were added to these statistics for the current
         * session.
         *
         * @return The number of DNF solves that were added for the current session.
         */
        get() = mOneSessionAC?.numDNFSolves ?: 0

    val sessionMeanTime: Long
        /**
         * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
         * statistics for the current session. The returned millisecond value is truncated to a whole
         * milliseconds value, not rounded.
         * 
         * @return
         * The mean time of all non-DNF solves that were added for the current session. The result
         * will be [AverageCalculator.UNKNOWN] if no times have been added, or if all added
         * times were DNFs.
         */
        get() = mOneSessionAC?.meanTime ?: AverageCalculator.UNKNOWN

    val sessionTotalTime: Long
        /**
         * Gets the total time (sum of all times) of all non-DNF solves that were added to these
         * statistics for the current session.
         * 
         * @return
         * The total time of all non-DNF solves that were added for the current session. The result
         * will be [AverageCalculator.UNKNOWN] if no times have been added, or if all added
         * times were DNFs.
         */
        get() = mOneSessionAC?.totalTime ?: AverageCalculator.UNKNOWN

    val sessionStdDeviation: Long
        /**
         * Gets the current Sample Standard Deviation of all non-DNF solves that were added to these
         * statistics for the current session.
         * 
         * @return
         * The current Sample Standard Deviation of all non-DNF solves that were added for the
         * current session. The result will be [AverageCalculator.UNKNOWN] if no times have
         * been added, or if all added times were DNFs.
         */
        get() = mOneSessionAC?.standardDeviation ?: AverageCalculator.UNKNOWN

    val sessionTimeFrequencies: MutableMap<Long, Int>
        /**
         * Gets the solve time frequencies for the current sessions. The times are truncated to whole
         * seconds, but still expressed as milliseconds. The keys are the times (and
         * [AverageCalculator.DNF] can be a key), and the values are the number of solves times
         * that fell into the one-second interval for that key. For example, if the key is "4", the
         * value is the number of solve times of "four-point-something seconds".
         * 
         * @return
         * The solve time frequencies. The iteration order of the map begins with any DNF solves
         * and then continues in increasing order of time value. This may be modified freely.
         */
        get() = TreeMap<Long, Int>(mSessionTimeFreqs)

    val allTimeBestTime: Long
        /**
         * Gets the best solve time of all those added to these statistics for a solve in all past
         * and current sessions.
         * 
         * @return
         * The best time ever added for all past and current sessions. The result will be
         * [AverageCalculator.UNKNOWN] if no times have been added, or if all added times
         * were DNFs.
         */
        get() = mOneAllTimeAC?.bestTime ?: AverageCalculator.UNKNOWN

    val allTimeWorstTime: Long
        /**
         * Gets the worst time (not a DNF) of all those added to these statistics for a solve in all
         * past and current sessions.
         * 
         * @return
         * The worst time ever added for all past and current sessions. The result will be
         * [AverageCalculator.UNKNOWN] if no times have been added, or if all added times
         * were DNFs.
         */
        get() = mOneAllTimeAC?.worstTime ?: AverageCalculator.UNKNOWN

    val allTimeNumSolves: Int
        /**
         * Gets the total number of solve times (including DNFs) that were added to these statistics
         * for all past and current sessions. To get the number of non-DNF solves, subtract the result
         * of [.getAllTimeNumDNFSolves].
         * 
         * @return The number of solve times that were added for all past and current sessions.
         */
        get() = mOneAllTimeAC?.numSolves ?: 0

    val allTimeNumDNFSolves: Int
        /**
         * Gets the total number of DNF solves that were added to these statistics for all past and
         * current sessions.
         * 
         * @return The number of DNF solves that were added for all past and current sessions.
         */
        get() = mOneAllTimeAC?.numDNFSolves ?: 0

    val allTimeMeanTime: Long
        /**
         * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
         * statistics for all past and current sessions. The returned millisecond value is truncated
         * to a whole milliseconds value, not rounded.
         * 
         * @return
         * The mean time of all non-DNF solves that were added for all past and current sessions.
         * The result will be [AverageCalculator.UNKNOWN] if no times have been added, or if
         * all added times were DNFs.
         */
        get() = mOneAllTimeAC?.meanTime ?: AverageCalculator.UNKNOWN

    val allTimeTotalTime: Long
        /**
         * Gets the total time (sum of all times) of all non-DNF solves that were added to these
         * statistics for all past and current sessions.
         * 
         * @return
         * The total time of all non-DNF solves that were added for all past and current sessions
         * The result will be [AverageCalculator.UNKNOWN] if no times have been added, or
         * if all added times were DNFs.
         */
        get() = mOneAllTimeAC?.totalTime ?: AverageCalculator.UNKNOWN

    val allTimeStdDeviation: Long
        /**
         * Gets the Sample Standard Deviation of all non-DNF solves that were added to these
         * statistics for all past and current sessions.
         * 
         * @return
         * The current Sample Standard Deviation of all non-DNF solves that were added for all
         * past and current sessions. The result will be [AverageCalculator.UNKNOWN] if no
         * times have been added, or if all added times were DNFs.
         */
        get() = mOneAllTimeAC?.standardDeviation ?: AverageCalculator.UNKNOWN

    companion object {
        /**
         * Percent to trim off each end
         */
        private var mTrimSize = 0

        /**
         * Creates a new set of statistical averages for the detailed table of all-time and session
         * statistics reported on the statistics/graph tab. Averages of 3, 5, 12, 50, 100 and 1,000 are
         * added for all sessions and for the current session only. The average of 3 permits no DNF
         * solves. The averages of 5 and 12 permit no more than one DNF solves. The averages of 50, 100
         * and 1,000 permit all but one solve to be a DNF solve.
         *
         * @return The detailed set of all-time solve time statistics for the statistics/graph tab.
         */
        fun newAllTimeStatistics(): Statistics {
            val stats = Statistics()

            mTrimSize =
                getInt(R.string.pk_stat_trim_size, getDefaultIntValue(R.integer.defaultTrimSize))

            // Averages for all sessions.
            stats.addAverageOf(3, 0, false)
            stats.addAverageOf(5, 5, false)
            stats.addAverageOf(12, 5, false)
            stats.addAverageOf(50, mTrimSize, false)
            stats.addAverageOf(100, mTrimSize, false)
            stats.addAverageOf(1000, mTrimSize, false)

            // Averages for the current session only.
            stats.addAverageOf(3, 0, true)
            stats.addAverageOf(5, 5, true)
            stats.addAverageOf(12, 5, true)
            stats.addAverageOf(50, mTrimSize, true)
            stats.addAverageOf(100, mTrimSize, true)
            stats.addAverageOf(1000, mTrimSize, true)

            return stats
        }

        /**
         * Creates a new set of statistical averages for the averages displayed in the graph of the
         * times from the all past and current sessions. Averages of 50 and 100 are added for the
         * *current session only* (a requirement for the [ChartStatistics] constructor, even
         * though the all-time data will be graphed). These averages permit all but one solves to be
         * DNFs.
         *
         * @return The solve time statistics for graphing the all-time averages.
         */
        fun newAllTimeAveragesChartStatistics(): Statistics {
            val stats = Statistics()

            // Averages for the current session only IS NOT A MISTAKE! The "ChartStatistics" class
            // passes all data in for the "current session", but makes a distinction using its own API.
            stats.addAverageOf(50, mTrimSize, true)
            stats.addAverageOf(100, mTrimSize, true)

            return stats
        }

        /**
         * Creates a new set of statistical averages for the averages displayed in the graph of the
         * times from the current session. Averages of 5 and 12 are added for the current session
         * only. These averages permit no more than one DNF solve.
         *
         * @return The solve time statistics for graphing the current session averages.
         */
        fun newCurrentSessionAveragesChartStatistics(): Statistics {
            val stats = Statistics()

            stats.addAverageOf(5, 5, true)
            stats.addAverageOf(12, 5, true)

            return stats
        }
    }
}
