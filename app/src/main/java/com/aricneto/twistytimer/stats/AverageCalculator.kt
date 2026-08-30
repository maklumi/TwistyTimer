package com.aricneto.twistytimer.stats

import android.util.Log
import com.aricneto.twistytimer.items.AverageComponent
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.StatUtils.asList
import java.util.Arrays
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Calculates the average time of a number of puzzle solves. Running averages are easily calculated
 * as each new solve is added. If the number of solve times is five or greater, the best and worst
 * times are discarded before returning the truncated arithmetic mean (aka "trimmed mean" or
 * "modified mean) of the remaining times. All times and averages are in milliseconds. The mean,
 * minimum (best) and maximum (worst) of all added times, and the best average from all values of
 * the running average are also made available.
 * 
 * @author damo
 */
class AverageCalculator internal constructor(n: Int, trimPercent: Int) {
    /**
     * Gets the number of solve times that are included in the average. This is inclusive of any
     * times, such as the best and worst times, that are excluded when calculating the truncated
     * mean. For example, for an "average of 12", the best time and the worst time are trimmed
     * before getting the truncated arithmetic mean of the remaining 10 times, but this method
     * returns 12. This may be greater than the number of times added so far and given by
     * [.getNumSolves].
     * 
     * @return The number of times that must be considered in the calculation of the average.
     */
    /**
     * The number of solve times to include in the average.
     */
    var n: Int = 1

    /**
     * Indicates if averages should be reported as [.DNF]s if too many solve times are DNFs.
     * The number of DNFs that constitute "too many" varies with the value of [.mN].
     */
    private val mDisqualifyDNFs: Boolean

    /**
     * The array holding the most recently added solve times. A solve time can also be recorded as
     * a [.DNF]. This is managed as a circular queue. Once full, the oldest added time is
     * overwritten when the next new time is added.
     */
    private val mTimes: LongArray

    var mUpperTrim: AverageComponent
    var mMiddleTrim: AverageComponent
    var mLowerTrim: AverageComponent
    private val mLowerTrimBound: Int
    private val mUpperTrimBound: Int
    private val mTrimSize: Int

    /**
     * The index in [.mTimes] at which to add the next time. If this is equal to the length
     * of the array, it will be wrapped back to zero.
     */
    private var mNext = 0

    /**
     * Gets the total number of solve times (including DNFs) that were added to this calculator.
     * This may be greater than the number (given by [.getN]) that are included in the
     * calculation of the average. Subtract the value from [.getNumDNFSolves] to get the
     * total number of non-DNF solves.
     * 
     * @return The number of solve times that were added to this calculator.
     */
    /**
     * The total number of solve times that have been added to the array. This may exceed
     * [.mN], but no more than that number of solve times will be stored at any one time.
     */
    var numSolves: Int = 0
        private set

    /**
     * The number of DNF results currently recorded in `#mTimes`.
     */
    private var mNumCurrentDNFs = 0

    /**
     * The maximum number of DNFs that can be contained in [.mTimes]
     * before the whole average is considered a DNF
     */
    private val mNumAcceptableDNFs: Int

    /**
     * Gets the total number of DNF solves that were added to this calculator.
     * 
     * @return The number of DNF solves that were added to this calculator.
     */
    /**
     * The number of DNF results ever recorded in `#mTimes`.
     */
    var numDNFSolves: Int = 0
        private set


    /**
     * The Welford algorithm for variance is one of the most well-known and used online methods of
     * calculating the variance of a given sample data. The algorithm itself has several
     * variables, which are listed here. I won't pretend to fully understand it myself, but there
     * are a lot of references online for it.
     */
    private var mMean = 0.0
    private var mVarianceDelta = 0.0
    private var mVarianceDelta2 = 0.0
    private var mVarianceM2 = 0.0

    /**
     * The current variance of all solves ever recorded. A value of [.UNKNOWN] indicates
     * the sample size is not enough for it to be calculated yet.
     */
    private var mVariance: Long = 0

    /**
     * The sum of all non-DNF results currently recorded in `#mTimes`. The number of such
     * results is given by `Math.min(mN, mNumSolves) - mNumCurrentDNFs`. A value of
     * [.UNKNOWN] indicates that there are no non-DNF results recorded.
     */
    private var mCurrentSum: Long = 0

    /**
     * Gets the total time of all non-DNF solves that were added to this calculator.
     * 
     * @return
     * The total time of all non-DNF solves that were added to this calculator. The result
     * will be [.UNKNOWN] if no times have been added, or if all added times were
     * [.DNF]s.
     */
    /**
     * The sum of all non-DNF results ever recorded in `#mTimes`. The number of such results
     * is given by `mNumSolves - mNumAllTimeDNFs`. A value of [.UNKNOWN] indicates
     * that there are no non-DNF results recorded.
     */
    var totalTime: Long = 0
        private set

    /**
     * The best time currently recorded in `#mTimes`. A value of [.UNKNOWN] indicates
     * that there is no non-DNF result recorded.
     */
    private var mCurrentBestTime: Long = 0

    /**
     * The worst time (not a DNF) currently recorded in `#mTimes`. If any DNF is present, a
     * DNF will be taken instead as the worst time, if the calculation needs to exclude one. A
     * value of [.UNKNOWN] indicates that there is no non-DNF result recorded.
     */
    private var mCurrentWorstTime: Long = 0

    /**
     * 
     * 
     * Gets the current value of the average. This is calculated from the most recently added
     * times. The number of times considered is given by [.getN] ("n").
     * 
     * 
     * 
     * Where the value of "n" is less than 5, the average is the arithmetic mean of the currently
     * stored values, not a truncated mean. If any currently recorded solve is a [.DNF],
     * the average is disqualified as a DNF unless configured so that DNFs do not automatically
     * disqualify averages (by passing `false` as the value of the `disqualifyDNFs`
     * parameter to [.AverageCalculator]). If DNFs are allowed, then the
     * average is the average time of all non-DNF solves, but will still be a DNF average if all
     * solves are DNFs.
     * 
     * 
     * 
     * Where the value of "n" is 5 or greater, the average is the truncated arithmetic mean of the
     * currently stored values. The single best and single worst solve times are discarded and the
     * average is calculated from the remaining times. If one DNF is present, it is taken as the
     * worst solve time and discarded. If more than one DNF is present, the average is disqualified
     * as a DNF unless configured so that DNFs do not automatically disqualify averages. If more
     * DNFs are allowed, one will be taken as the worst solve time and the other DNFs will be
     * ignored. If only a single non-DNF time remains, it will not be discarded as the best time
     * and will be returned as the average time.
     * 
     * 
     * @return
     * The current (truncated( arithmetic mean of the most recently added values. If fewer
     * times have been added that the number required, the result will be [.UNKNOWN].
     * If too many DNF solves are included in the recently-added times, the result is
     * `DNF`. The returned integer value of the average is truncated (rounded down).
     */
    /**
     * The current average value calculated from all times stored in [.mTimes]. A value of
     * [.UNKNOWN] indicates insufficient results have been added to calculate the required
     * average, or that the calculation has not been performed. A value of [.DNF] indicates
     * that too many DNF results are present and the average is disqualified.
     */
    var currentAverage: Long = 0
        private set

    /**
     * Gets the best time of all those added to this calculator.
     * 
     * @return
     * The best time ever added to this calculator. The result will be [.UNKNOWN] if no
     * times have been added, or if all added times were [.DNF]s.
     */
    /**
     * The best time ever added to this calculator. This time may not currently be recorded in
     * `#mTimes`, as it may have been overwritten. A value of [.UNKNOWN] indicates that
     * there is no non-DNF result recorded.
     */
    var bestTime: Long = 0
        private set

    /**
     * Gets the worst time (not a DNF) of all those added to this calculator.
     * 
     * @return
     * The worst time ever added to this calculator. The result will be [.UNKNOWN] if no
     * times have been added, or if all added times were [.DNF]s.
     */
    /**
     * The worst time (not a DNF) ever added to this calculator. This time may not currently be
     * recorded in `#mTimes`, as it may have been overwritten. A value of [.UNKNOWN]
     * indicates that there is no non-DNF result recorded.
     */
    var worstTime: Long = 0
        private set

    /**
     * Gets the best value of the average. This is calculated across all added times. The number of
     * times considered is given by [.getN]. The average for each consecutive sequence of
     * that number of times (including DNFs) is calculated as each new time is added and the best
     * average of all of those sequences is returned. See [.getCurrentAverage] for more
     * details.
     * 
     * @return
     * The best truncated arithmetic mean across all added values. If fewer times have been
     * added that the number required, the result will be [.UNKNOWN]. If too many DNF
     * solves are included in *all* sequences of times, the result is [.DNF].
     */
    /**
     * The best average value calculated from all times added to date. A value of [.UNKNOWN]
     * indicates that insufficient results have been added to calculate the required average. A
     * value of [.DNF] indicates that averages could be calculated, but that every average
     * was disqualified as a DNF average.
     */
    var bestAverage: Long = 0
        private set

    /**
     * Creates a new calculator for the "average of *n*" solve times.
     * 
     * @param n
     * The number of solve times that will be averaged (e.g., 3, 5, 12, ...). Must be greater
     * than zero.
     * 
     * @throws IllegalArgumentException
     * If `n` is not greater than zero.
     */
    init {
        require(n > 0) { "Number of solves must be > 0: " + n }

        this.n = n
        mTimes = LongArray(n)
        mDisqualifyDNFs = true

        mTrimSize = ceil((this.n * (trimPercent / 100f)).toDouble()).toInt()
        mNumAcceptableDNFs = mTrimSize
        mLowerTrimBound = mTrimSize
        mUpperTrimBound = this.n - mTrimSize

        mUpperTrim = AverageComponent()
        mMiddleTrim = AverageComponent()
        mLowerTrim = AverageComponent()

        // As "reset()" needs to be supported to ensure a sane state can be guaranteed before
        // populating statistics from the database, it makes sense to use it to initialise the
        // fields in one place.
        reset()
    }

    /**
     * Resets all statistics and averages that have been collected previously.
     */
    fun reset() {
        Arrays.fill(mTimes, 0L)
        mNext = 0
        this.numSolves = 0
        mNumCurrentDNFs = 0
        this.numDNFSolves = 0

        // Variance variables
        mMean = 0.0
        mVarianceDelta = 0.0
        mVarianceDelta2 = 0.0
        mVarianceM2 = 0.0

        mMiddleTrim = AverageComponent()
        mLowerTrim = AverageComponent()
        mUpperTrim = AverageComponent()

        mCurrentSum = UNKNOWN
        this.totalTime = UNKNOWN
        mCurrentBestTime = UNKNOWN
        mCurrentWorstTime = UNKNOWN
        this.currentAverage = UNKNOWN
        this.bestTime = UNKNOWN
        this.worstTime = UNKNOWN
        this.bestAverage = UNKNOWN
        mVariance = UNKNOWN
    }

    /**
     * Adds a solve time to be included in the calculation of the average. Solve times should be
     * added in chronological order (i.e., by solve time-stamp, not solve time).
     * 
     * @param time
     * The solve time in milliseconds. The time must be greater than zero. Use [.DNF] to
     * represent a DNF solve.
     * 
     * @throws IllegalArgumentException
     * If the added time is not greater than zero and is not `DNF`.
     */
    @Throws(IllegalArgumentException::class)
    fun addTime(time: Long) {
        if (time <= 0L && time != DNF) {
            // FIXME: throwing an IllegalArgumentException here is too harsh for the user. If the app
            // incorrectly imports an illegal solve, the app will keep crashing and the only way for
            // the user to fix this is to clear the app data. I'm commenting this off for the time
            // being until the import algorithm gets sorted out.
            // TODO: Should the app automatically remove illegal solves?

            Log.e("AverageCalculator", "Time must be > 0 or be 'DNF': " + time)

            // throw new IllegalArgumentException("Time must be > 0 or be 'DNF': " + time);
        } else {
            this.numSolves++

            val ejectedTime: Long

            // If the array has just been filled, store a sorted version of it
            // If the array is full, "mNext" points to the oldest result that needs to be ejected first.
            //      We also need to remove the oldest solve from the sorted array and insert the new solve
            // If the array is not full, then "mNext" points to an empty entry, so no special handling
            // is needed.
            if (this.numSolves >= this.n) {
                if (mNext == this.n) {
                    // Need to wrap around to the start (index zero).
                    mNext = 0
                }
                ejectedTime = mTimes[mNext] // May be DNF.

                // Create the sorted list as soon as numSolves reaches N
                // We only need to sort it once. Subsequent added solves will use
                // an algorithm to insert the new solves in the correct (sorted) position
                if (this.numSolves == this.n) {
                    mTimes[mNext] = time

                    // Sort mTimes
                    val sortedTimes: MutableList<Long> = ArrayList<Long>(asList(mTimes))
                    Collections.sort<Long>(sortedTimes)

                    // Distribute the sorted times into the trims
                    var count = 0
                    for (solve in sortedTimes) {
                        if (count < mLowerTrimBound) mLowerTrim.put(solve)
                        else if (count >= mUpperTrimBound) mUpperTrim.put(solve)
                        else mMiddleTrim.put(solve)
                        count++
                    }
                }
            } else {
                // "mNext" must be less than "mN" if "mNumSolves" is less than "mN".
                ejectedTime = UNKNOWN // Nothing ejected.
            }

            mTimes[mNext] = time
            mNext++

            //Log.d("AverageCalculator", "N: " + mN + " | Set: " + mSortedTimes);

            // Order is important here, as these methods change fields and some methods depend on the
            // fields being updated by other methods before they are called. All depend on the new
            // time being stored already (see above) and any ejected time being known (also above).
            updateDNFCounts(time, ejectedTime)
            updateCurrentBestAndWorstTimes(time, ejectedTime)
            updateSums(time, ejectedTime)
            updateCurrentTrims(time, ejectedTime)
            updateVariance(time)
            updateCurrentAverage()

            updateAllTimeBestAndWorstTimes()
            updateAllTimeBestAverage()
        }
    }

    /**
     * Adds solve times to be included in the calculation of the average. Solve times should be
     * added in chronological order (i.e., by solve time-stamp, not solve time). This method can be
     * called repeatedly to add any number of solve times over any number of calls.
     * 
     * @param times
     * Zero or more solve times in milliseconds. Times must be greater than zero. Use
     * [.DNF] to represent each DNF. If this is `null` or empty, it will be ignored
     * and this method will have no effect.
     * 
     * @throws IllegalArgumentException
     * If any added time is not greater than zero and is not `DNF`.
     */
    @Throws(IllegalArgumentException::class)
    fun addTimes(vararg times: Long) {
        // The variable arguments list makes it easier to write compact test cases; it does not
        // really make life any easier when adding times via a database cursor. In non-test
        // contexts, it will be more efficient to call "addTime", as each call will not need to
        // create a "long[]" object.
        if (times != null) {
            for (time in times) {
                addTime(time) // May throw IAE.
            }
        }
    }

    /**
     * Updates the current and all-time counts of DNF solves.
     * 
     * @param addedTime
     * The newly added time. May be [.DNF].
     * @param ejectedTime
     * An old time that was ejected to make room for the newly added time. May be `DNF`.
     * Use [.UNKNOWN] if no old time was ejected.
     */
    private fun updateDNFCounts(addedTime: Long, ejectedTime: Long) {
        if (addedTime == DNF) {
            mNumCurrentDNFs++
            this.numDNFSolves++
        }

        if (ejectedTime == DNF) {
            mNumCurrentDNFs--
        }
    }

    /**
     * Updates the current best and worst times after a new time is added. The count of DNFs must
     * be updated by [.updateDNFCounts] before calling this method.
     * 
     * @param addedTime
     * The newly added time. May be [.DNF]. Must already be stored.
     * @param ejectedTime
     * An old time that was ejected to make room for the newly added time. May be `DNF`.
     * Use [.UNKNOWN] if no old time was ejected.
     */
    private fun updateCurrentBestAndWorstTimes(addedTime: Long, ejectedTime: Long) {
        // The logic here will set one or both of "mCurrentBestTime" and "mCurrentWorstTime" to
        // "UNKNOWN" if knowledge of the best or worst time has been lost. If either value becomes
        // "UNKNOWN", a new iteration over "mTimes" will recalculate both values.

        if (addedTime == DNF) {
            // Newly added time does not change the current best or worst time, but has either of
            // the best or worst times (not a DNF) just been ejected and is recalculation required?
            if (ejectedTime == mCurrentBestTime || ejectedTime == mCurrentWorstTime) {
                // It does not matter which has been ejected, just recalculate both.
                mCurrentBestTime = UNKNOWN
                mCurrentWorstTime = UNKNOWN
            }
        } else {
            // Newly added time is not a DNF and may be the new best or worst time (or both).
            // However, if it is not the new (or equal) best or worst time, then check if we are
            // ejecting the old best or worst time. If either is ejected, there may be another best
            // or worst time in "mTimes" (with respect to "addedTime") and it must be found. There
            // if no need to check if "ejectedTime" is DNF or UNKNOWN.
            if (mCurrentBestTime == UNKNOWN || addedTime <= mCurrentBestTime) {
                mCurrentBestTime = addedTime
            } else if (ejectedTime == mCurrentBestTime) {
                mCurrentBestTime = UNKNOWN
            }

            if (mCurrentWorstTime == UNKNOWN || addedTime >= mCurrentWorstTime) {
                mCurrentWorstTime = addedTime
            } else if (ejectedTime == mCurrentWorstTime) {
                mCurrentWorstTime = UNKNOWN
            }
        }

        // Recalculate the best and worst times. We can skip this if every stored time is a DNF.
        // In that case, both "mCurrentBestTime" and "mCurrentWorstTime" will remain UNKNOWN.
        val numCurrentSolves = min(this.numSolves, this.n)

        if (mNumCurrentDNFs != numCurrentSolves
            && (mCurrentBestTime == UNKNOWN || mCurrentWorstTime == UNKNOWN)
        ) {
            // At least one stored time is not a DNF and is > 0, so reset the fields and rescan.
            mCurrentBestTime = Long.MAX_VALUE
            mCurrentWorstTime = 0L

            // There is no need to follow the chronological insertion order here. The array may
            // not be full yet.
            for (i in 0..<numCurrentSolves) {
                val time = mTimes[i]

                if (time != DNF) {
                    mCurrentBestTime = min(mCurrentBestTime, time)
                    mCurrentWorstTime = max(mCurrentWorstTime, time)
                }
            }
        }
    }

    private fun updateCurrentTrims(addedTime: Long, ejectedTime: Long) {
        if (this.numSolves > this.n && mLowerTrimBound > 0) {
            // Ejected time belongs to lower trim
            if (ejectedTime <= mLowerTrim.getGreatest()) {
                // Remove the ejected time
                mLowerTrim.remove(ejectedTime)

                if (addedTime <= mMiddleTrim.getLeast()) {
                    // Added time belongs to lower trim
                    mLowerTrim.put(addedTime)
                } else if (addedTime >= mUpperTrim.getLeast()) {
                    // Added time belongs to upper trim
                    // Move least elements to the left
                    mLowerTrim.put(mMiddleTrim.getLeast())
                    mMiddleTrim.remove(mMiddleTrim.getLeast())
                    mMiddleTrim.put(mUpperTrim.getLeast())
                    mUpperTrim.remove(mUpperTrim.getLeast())
                    mUpperTrim.put(addedTime)
                } else {
                    // Added time belongs to middle trim
                    // Move least elements to the left
                    mLowerTrim.put(mMiddleTrim.getLeast())
                    mMiddleTrim.remove(mMiddleTrim.getLeast())
                    mMiddleTrim.put(addedTime)
                }
            } else if (ejectedTime >= mUpperTrim.getLeast()) {
                // Remove the ejected time
                mUpperTrim.remove(ejectedTime)

                if (addedTime >= mMiddleTrim.getGreatest()) {
                    // Added time belongs to upper trim
                    mUpperTrim.put(addedTime)
                } else if (addedTime <= mLowerTrim.getGreatest()) {
                    // Added time belongs to lower trim
                    // Move greatest elements to right
                    mUpperTrim.put(mMiddleTrim.getGreatest())
                    mMiddleTrim.remove(mMiddleTrim.getGreatest())
                    mMiddleTrim.put(mLowerTrim.getGreatest())
                    mLowerTrim.remove(mLowerTrim.getGreatest())
                    mLowerTrim.put(addedTime)
                } else {
                    // Added time belongs to middle trim
                    // Move greatest elements to right
                    mUpperTrim.put(mMiddleTrim.getGreatest())
                    mMiddleTrim.remove(mMiddleTrim.getGreatest())
                    mMiddleTrim.put(addedTime)
                }
            } else {
                // Remove the ejected time
                mMiddleTrim.remove(ejectedTime)

                if (addedTime >= mUpperTrim.getLeast()) {
                    // Added time belongs to upper trim
                    // Move least elements to left
                    mMiddleTrim.put(mUpperTrim.getLeast())
                    mUpperTrim.remove(mUpperTrim.getLeast())
                    mUpperTrim.put(addedTime)
                } else if (addedTime <= mLowerTrim.getGreatest()) {
                    // Added time belongs to lower trim
                    // Move greatest elements to right
                    mMiddleTrim.put(mLowerTrim.getGreatest())
                    mLowerTrim.remove(mLowerTrim.getGreatest())
                    mLowerTrim.put(addedTime)
                } else {
                    // Added time belongs to middle trim
                    mMiddleTrim.put(addedTime)
                }
            }
        } else if (this.numSolves > this.n) {
            // If the bound is 0, mLowerTrim and mUpperTrim will be null
            // All operations will be done on mMiddleTrim
            mMiddleTrim.remove(ejectedTime)
            mMiddleTrim.put(addedTime)
        }
    }

    /**
     * Updates the sum of all times currently stored and the sum of all times ever added. Any
     * [.DNF] results are ignored. If all recorded times have been DNFs, the sums will set
     * to [.UNKNOWN].
     * 
     * @param addedTime
     * The newly added time. May be `DNF`. Must already be stored.
     * @param ejectedTime
     * An old time that was ejected to make room for the newly added time. May be `DNF`.
     * Use `UNKNOWN` if no old time was ejected.
     */
    private fun updateSums(addedTime: Long, ejectedTime: Long) {
        if (addedTime != DNF) {
            mCurrentSum = addedTime + (if (mCurrentSum == UNKNOWN) 0L else mCurrentSum)
            this.totalTime = addedTime + (if (this.totalTime == UNKNOWN) 0L else this.totalTime)
        }
        if (ejectedTime != DNF && ejectedTime != UNKNOWN) {
            mCurrentSum -= ejectedTime
        }

        // Returned from a state with at least one non-DNF time to a state where all times are DNFs.
        // Flag the new state properly. ("mAllTimeSum" cannot return to zero.)
        if (mCurrentSum == 0L) {
            mCurrentSum = UNKNOWN
        }
    }

    /**
     * The Welford algorithm for variance is one of the most well-known and used online methods of
     * calculating the variance of a given sample data. I won't pretend to fully understand it
     * myself, but there are a lot of references online for it.
     */
    private fun updateVariance(addedTime: Long) {
        val totalValidSolves = (this.numSolves - this.numDNFSolves).toLong()
        if (addedTime != DNF) {
            mVarianceDelta = (addedTime.toDouble()) - mMean
            mMean += mVarianceDelta / totalValidSolves
            mVarianceDelta2 = (addedTime.toDouble()) - mMean
            mVarianceM2 += mVarianceDelta * mVarianceDelta2
        }
        if (totalValidSolves > 2) {
            mVariance = (mVarianceM2 / (totalValidSolves - 1)).toLong()
        }
    }

    /**
     * Updates the average value of the most recently added times. See [.getCurrentAverage]
     * for details. The sum, best and worst values and other fields must be updated by first calling
     * [.updateSums] and [.updateCurrentBestAndWorstTimes] and
     * their dependent methods before calling this method.
     */
    private fun updateCurrentAverage() {
        if (this.numSolves < this.n) {
            // Not enough times added to calculate the average.
            this.currentAverage = UNKNOWN
        } else if (mNumCurrentDNFs == this.n) {
            // Enough times have been added, but all of the currently stored ones are DNFs.
            this.currentAverage = DNF
        } else if (!mDisqualifyDNFs && mNumCurrentDNFs == this.n - 1) {
            // More than one DNF is not an automatic disqualification, but there is only one
            // non-DNF time present. Just use that time as the average.
            this.currentAverage = mCurrentBestTime
        } else if (this.n >= MIN_N_TO_ALLOW_ONE_DNF) {
            if (mDisqualifyDNFs && mNumCurrentDNFs > mNumAcceptableDNFs) {
                // Disqualify the average: the number of current DNFs is above the acceptable threshold
                this.currentAverage = DNF
            } else {
                // There is no more than one DNF, or there is more than one DNF, but that will not
                // cause automatic disqualification. There are at least two non-DNF times present.
                // Calculate a truncated arithmetic mean. "mCurrentSum" is the sum of all non-DNF
                // times. Discard the upper and lower trims, discard all other DNFs, if any.
                // One DNF may already have been discarded as the worst time; do not discard it twice.
                this.currentAverage = mMiddleTrim.getSum() /
                        (this.n - (mTrimSize * 2) -
                                (if (mNumCurrentDNFs > 1) mNumCurrentDNFs - 1 else 0))
            }
        } else { // mN < MIN_N_TO_ALLOW_ONE_DNF
            // NOTE: "mN" could be as low as 1, but will not be zero (see the constructor).
            if (mDisqualifyDNFs && mNumCurrentDNFs > 0) {
                // Disqualify the average as *no* DNF (not even one) is allowed for small "n".
                this.currentAverage = DNF
            } else {
                // There is no DNF, or there are DNFs, but that will not cause automatic
                // disqualification. There is at least one non-DNF time present. Calculate the
                // (not truncated) arithmetic mean.
                this.currentAverage = mCurrentSum / (this.n - mNumCurrentDNFs)
            }
        }
    }

    /**
     * Updates the all-time best and worst times after a new time is added. The current best and
     * worst times must be updated by [.updateCurrentBestAndWorstTimes] before
     * calling this method.
     */
    private fun updateAllTimeBestAndWorstTimes() {
        if (this.bestTime == UNKNOWN) {
            this.bestTime = mCurrentBestTime // May still be UNKNOWN.
        } else if (mCurrentBestTime != UNKNOWN) {
            // "mCurrentBestTime" is never set to "DNF".
            this.bestTime = min(this.bestTime, mCurrentBestTime)
        }

        if (this.worstTime == UNKNOWN) {
            this.worstTime = mCurrentWorstTime // May still be UNKNOWN.
        } else if (mCurrentWorstTime != UNKNOWN) {
            this.worstTime = max(this.worstTime, mCurrentWorstTime)
        }
    }

    /**
     * Updates the all-time best average after a new time is added. The current average must be
     * updated by [.updateCurrentAverage] before calling this method.
     */
    private fun updateAllTimeBestAverage() {
        if (this.bestAverage == UNKNOWN || this.bestAverage == DNF) {
            // "mCurrentAverage" may still be UNKNOWN or DNF, but cannot change back to UNKNOWN once
            // set to a different value, as UNKNOWN is cleared once "mN" solves have been added.
            // Therefore, we never set "mAllTimeBestAverage" to a value worse than it already has.
            this.bestAverage = this.currentAverage
        } else if (this.currentAverage != DNF) {
            this.bestAverage = min(this.bestAverage, this.currentAverage)
        }
    }

    val standardDeviation: Long
        /**
         * Gets the current Sample Standard Deviation of all non-DNF solves that were added to this
         * calculator
         * 
         * @return
         * The Sample Standard Deviation of all non-DNF solves that were added to this calculator
         * Will be [.UNKNOWN] if no times have been added, or if all added solve times
         * were [.DNF]s.
         */
        get() = if (mVariance != UNKNOWN) sqrt(mVariance.toDouble())
            .toLong() else UNKNOWN

    val meanTime: Long
        /**
         * Gets the simple arithmetic mean time of all non-DNF solves that were added to this
         * calculator. The returned millisecond value is truncated to a whole milliseconds value, not
         * rounded.
         * 
         * @return
         * The mean time of all non-DNF solves that were added to this calculator. The result
         * will be [.UNKNOWN] if no times have been added, or if all added times were
         * [.DNF]s.
         */
        get() = if (mMean.toLong() != 0L) mMean.toLong() else UNKNOWN

    val averageOfN: AverageOfN
        /**
         * Captures the details of the average-of-N calculation including the most recently added time.
         * 
         * @return The details for the average-of-N calculation.
         */
        get() = AverageOfN(this)

    /**
     * A summary of the average of the mostly recently added times. All times are provided in an
     * array and the calculated average, best time and worst time are identified, if appropriate.
     */
    class AverageOfN(ac: AverageCalculator) {
        /**
         * Gets the array of values that contributed to the calculation of the average-of-N. If
         * too few values have been recorded (less than "N"), the array will be `null`. The
         * times will be ordered with the oldest recorded time first and may include [.DNF]
         * values. The best and worst times can be identified with [.getBestTimeIndex] and
         * [.getWorstTimeIndex].
         * 
         * @return
         * The array of times used to calculate the average. May be `null`.
         */
        /**
         * The array of values that contributed to the calculation of the average-of-N. If too few
         * values have been recorded (less than "N"), the array will be `null`. The times
         * will be ordered with the oldest recorded time first.
         */
        val times: LongArray

        /**
         * The total sum of the upper-trim of the calculated times. May be [.DNF] if there are too
         * many DNF solves, or [.UNKNOWN] if they are too few times (less than "N").
         */
        private val mUpperTrimSum: Long

        /**
         * The total sum of the middle-trim of the calculated times. May be [.DNF] if there are too
         * many DNF solves, or [.UNKNOWN] if they are too few times (less than "N").
         */
        private val mMiddleTrimSum: Long

        /**
         * The total sum of the lower-trim of the calculated times. May be [.DNF] if there are too
         * many DNF solves, or [.UNKNOWN] if there are too few times (less than "N").
         */
        private val mLowerTrimSum: Long

        /**
         * Gets the index within the array returned by [.getTimes] of the best time
         * eliminated for the average-of-N calculation. If there were insufficient times, or if the
         * value of "N" is lower than the threshold where the best times are eliminated, or if there
         * are less than two non-DNF times, the result will be -1. When DNFs do not disqualify the
         * average and there is only one non-DNF time, that time is not identified as the "best"
         * time, so it is not eliminated; instead that single time becomes the average time.
         * 
         * @return The index of the best time value, or -1 if it is not known.
         */
        /**
         * The index within [.mTimes] of the best time. The index will be -1 if that array is
         * `null`, or if the average calculation for the value of "N" does not eliminate the
         * best time, or if all of the times are DNFs, or if DNFs do not cause disqualification,
         * but there is only one non-DNF time recorded.
         */
        val bestTimeIndex: Int

        /**
         * Gets the index within the array returned by [.getTimes] of the worst time
         * eliminated for the average-of-N calculation. If there were insufficient times, or if the
         * value of "N" is lower than the threshold where worst times are eliminated, or if all of
         * the times are DNF times, the result will be -1. If DNFs are present and at least one
         * time is not a DNF, the first DNF will be marked as the worst time.
         * 
         * @return The index of the worst time value, or -1 if it is not known.
         */
        /**
         * The index within [.mTimes] of the worst time. The index will be -1 if that array is
         * `null`, or if the average calculation for the value of "N" does not eliminate the
         * best time, or if all of the times are DNFs. The worst time may be a DNF.
         */
        val worstTimeIndex: Int

        /**
         * Gets the calculated average-of-N value. The calculation follows the normal rules for
         * the value of "N" that are applied by the average calculator from which this object was
         * captured.
         * 
         * @return
         * The average-of-N value. May be [.DNF] if the average was disqualified, or
         * [.UNKNOWN] if too few times have been recorded (i.e., less than "N").
         */
        /**
         * The average-of-N value calculated for the times. May be [.DNF] if there are too
         * many DNF solves, or [.UNKNOWN] if the are too few times (less than "N").
         */
        val average: Long

        /**
         * Creates a new record of the most recent "average-of-N" recorded by an average calculator.
         * 
         * @param ac The average calculator from which to capture the information.
         */
        init {
            val n = ac.n

            this.average = ac.currentAverage
            mUpperTrimSum = ac.mUpperTrim.getSum()
            mMiddleTrimSum = ac.mMiddleTrim.getSum()
            mLowerTrimSum = ac.mLowerTrim.getSum()

            if (this.average != UNKNOWN && ac.numSolves >= n) {
                this.times = LongArray(n)

                // The oldest time recorded in "ac.mTimes" is not necessarily the first one, as
                // that array operates as a circular queue. "ac.mNext" marks one index past the
                // last added time. However, the array should be full, so this should also be the
                // index of the first (oldest) time. If "ac.mNext" equals "n", then we wrap around
                // to start at index zero.
                val oldestIndex = if (ac.mNext == n) 0 else ac.mNext

                System.arraycopy(ac.mTimes, oldestIndex, this.times, 0, n - oldestIndex)
                System.arraycopy(ac.mTimes, 0, this.times, n - oldestIndex, oldestIndex)

                // "-1" is the convention for an unknown *index*, so "UNKNOWN" is not used.
                var bestIdx = -1
                var worstIdx = -1

                // IF the threshold value of "N" for the calculation of a truncated mean is not
                // reached, no best or worst times will be identified for elimination.
                //
                // IF all times are DNFs, no best or worst times will be identified for elimination.
                //
                // IF only one time is not a DNF, no best time will be identified for elimination.
                //
                // IF DNFs are present, a DNF will be identified as the worst time instead of
                // "mCurrentWorstTime", as "mCurrentWorstTime" is never set to DNF.
                if (n >= MIN_N_TO_ALLOW_ONE_DNF && n > ac.mNumCurrentDNFs) { // At least 1 non-DNF.
                    // Do not identify the only non-DNF time as the best time.
                    val bestTime = if (n - ac.mNumCurrentDNFs > 1) ac.mCurrentBestTime else UNKNOWN
                    // Identify a DNF as the worst time if DNFs are present.
                    val worstTime = if (ac.mNumCurrentDNFs == 0) ac.mCurrentWorstTime else DNF

                    var i = 0
                    while (i < n && (bestIdx == -1 || worstIdx == -1)) {
                        // Use if...else... here to ensure that the best and worst times are not
                        // recorded at the same index (e.g., if all times are DNFs or all equal).
                        if (bestIdx == -1 && this.times[i] == bestTime) {
                            bestIdx = i
                        } else if (worstIdx == -1 && this.times[i] == worstTime) {
                            worstIdx = i
                        }
                        i++
                    }
                }

                this.bestTimeIndex = bestIdx
                this.worstTimeIndex = worstIdx
            } else {
                this.times = LongArray(0)
                this.bestTimeIndex = -1
                this.worstTimeIndex = -1
            }
        }

        /**
         * Gets the calculated upper trim sum. That is, the sum of all times in sorted order
         * from the set trim boundary to the end of the mSortedList array
         * 
         * @return
         * The trim value. May be [.UNKNOWN] if too few times have been recorded (i.e., less than "N").
         */
        fun getmUpperTrimSum(): Long {
            return mUpperTrimSum
        }

        /**
         * Gets the calculated middle trim sum. That is, the sum of all times minus
         * `mUpperTrimSum` and `mLowerTrimSum`
         * @return
         * The trim value. May be [.UNKNOWN] if too few times have been recorded (i.e., less than "N").
         */
        fun getmMiddleTrimSum(): Long {
            return mMiddleTrimSum
        }

        /**
         * Gets the calculated lower trim sum. That is, the sum of all times in sorted order
         * from beginning mSortedList array until the set trim boundary
         * 
         * @return
         * The trim value. May be [.UNKNOWN] if too few times have been recorded (i.e., less than "N").
         */
        fun getmLowerTrimSum(): Long {
            return mLowerTrimSum
        }
    }

    companion object {
        // NOTE: This implementation is reasonably efficient, as it can calculate an average without
        // iterating over the array of recorded times. Iteration is only required when the known best
        // or worst values are ejected to make room for new times. Storing times in sorted order would
        // reduce this explicit iteration, but a second data structure would be required to record the
        // insertion order and the insertion operations would be more costly, as the sort order would
        // need to be maintained.
        //
        // Discarding only the single best and single worst values is standard for average-of-5
        // calculations, but for higher values of "n", it might be better to exclude, say, the best 10%
        // and worst 10%. However, that would make the implementation significantly more complicated if
        // arrays of best and worst values (or the limits of the ranges of each set of values and the
        // sums of those sets) were to be maintained. One approach might be to support calculation of
        // the median value instead, which would be relatively simple to do: sort the times, eliminate
        // the allowed DNFs and then find the time value in the middle position of the remaining times
        // in their sorted order (or the mean of the two middle values, if there are an even number of
        // remaining times).
        //
        // There are several alternative "streaming" algorithms that would not require the times to
        // be stored. However, all of those would introduce some amount of error due to rounding or
        // precision errors. As the largest value of "n" is likely to be 1,000, the approach used here
        // should be good enough and not use memory excessively.
        /**
         * A special time value that represents a solve that "did-not-finish" (DNF). This is also used
         * to represent the calculated value of an average where too many solves included in the
         * average were DNF solves.
         */
        // Deliberately avoiding using "PuzzleUtils.TIME_DNF" as the canonical flag value, as there is
        // no guarantee that it will remain with its current value of "-1" and this class needs to be
        // sure that the value will be negative and will not clash with "UNKNOWN". If changing these,
        // use values that can also be represented as in "int".
        val DNF: Long = Int.MAX_VALUE.toLong()

        /**
         * A value that indicates that a calculated time is unknown. This is usually the case when not
         * enough times have been recorded to satisfy the required number of solves to be included in
         * the calculation of the average, or when all recorded solves are [.DNF]s.
         */
        val UNKNOWN: Long = -666L

        /**
         * The minimum number of times to include in the average before a single DNF will not result
         * in disqualification.
         */
        private const val MIN_N_TO_ALLOW_ONE_DNF = 5

        /**
         * Translates a time value that may be [.UNKNOWN] or [.DNF] into a time value
         * that is compatible with methods such as the `PuzzleUtils.convertTimeToString*`
         * methods.
         * 
         * @param time The time value to be translated.
         * 
         * @return
         * The translated time value; `UNKNOWN` is translated to zero and `DNF` is
         * translated to [PuzzleUtils.TIME_DNF].
         */
        fun tr(time: Long): Long {
            if (time == UNKNOWN) {
                return 0L
            }
            if (time == DNF) {
                return PuzzleUtils.TIME_DNF
            }
            return time
        }
    }
}
