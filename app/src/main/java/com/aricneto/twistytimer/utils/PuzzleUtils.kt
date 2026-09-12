package com.aricneto.twistytimer.utils

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.stats.AverageCalculator
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.tr
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.PuzzleUtils.getPuzzleInPosition
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Created by Ari on 17/01/2016.
 */
object PuzzleUtils {
    const val TYPE_222 = "222"
    const val TYPE_333 = "333"
    const val TYPE_444 = "444"
    const val TYPE_555 = "555"
    const val TYPE_666 = "666"
    const val TYPE_777 = "777"
    const val TYPE_MEGA = "mega"
    const val TYPE_PYRA = "pyra"
    const val TYPE_SKEWB = "skewb"
    const val TYPE_CLOCK = "clock"
    const val TYPE_SQUARE1 = "sq1"

    const val NO_PENALTY = 0
    const val PENALTY_PLUSTWO = 1
    const val PENALTY_DNF = 2

    // The following penalty is a workaround to implement subtypes in the timer
    // Every time query should ignore every time that has a penalty of 10
    const val PENALTY_HIDETIME = 10
    const val TIME_DNF = -1L

    // -- Format constants for timeToString --
    const val FORMAT_DEFAULT = 0
    const val FORMAT_SMALL_MILLI = 1
    const val FORMAT_NO_MILLI = 2
    const val FORMAT_LARGE = 3

    @JvmStatic
    fun getPuzzleInPosition(position: Int): String {
        // IMPORTANT: Keep this in sync with the order in "R.array.puzzles".
        return when (position) {
            0 -> TYPE_222
            1 -> TYPE_333
            2 -> TYPE_444
            3 -> TYPE_555
            4 -> TYPE_666
            5 -> TYPE_777
            6 -> TYPE_SKEWB
            7 -> TYPE_MEGA
            8 -> TYPE_PYRA
            9 -> TYPE_SQUARE1
            10 -> TYPE_CLOCK
            else -> TYPE_222
        }
    }

    /**
     * Gets the position of the given puzzle type when presented in a spinner or other list. This
     * is the inverse of [getPuzzleInPosition].
     *
     * @param puzzleType The name of the type of puzzle.
     * @return The position (zero-based) of the puzzle within a list.
     */
    @JvmStatic
    fun getPositionOfPuzzle(puzzleType: String?): Int {
        // IMPORTANT: Keep this in sync with the order in "R.array.puzzles".
        return when (puzzleType) {
            TYPE_222 -> 0
            TYPE_333 -> 1
            TYPE_444 -> 2
            TYPE_555 -> 3
            TYPE_666 -> 4
            TYPE_777 -> 5
            TYPE_SKEWB -> 6
            TYPE_MEGA -> 7
            TYPE_PYRA -> 8
            TYPE_SQUARE1 -> 9
            TYPE_CLOCK -> 10
            else -> 0
        }
    }

    /**
     * Gets the string id of the name of a puzzle
     *
     * @param puzzle
     * @return
     */
    @JvmStatic
    @StringRes
    fun getPuzzleName(puzzle: String?): Int {
        return when (puzzle) {
            TYPE_333 -> R.string.cube_333_informal
            TYPE_222 -> R.string.cube_222_informal
            TYPE_444 -> R.string.cube_444_informal
            TYPE_555 -> R.string.cube_555_informal
            TYPE_666 -> R.string.cube_666_informal
            TYPE_777 -> R.string.cube_777_informal
            TYPE_CLOCK -> R.string.cube_clock
            TYPE_MEGA -> R.string.cube_mega
            TYPE_PYRA -> R.string.cube_pyra
            TYPE_SKEWB -> R.string.cube_skewb
            TYPE_SQUARE1 -> R.string.cube_sq1
            else -> 0
        }
    }

    @JvmStatic
    @StringRes
    fun getPuzzleNameFromType(puzzle: String?): Int {
        // IMPORTANT: Keep this in sync with the order in "R.array.puzzles".
        return when (puzzle) {
            TYPE_333 -> R.string.cube_333
            TYPE_222 -> R.string.cube_222
            TYPE_444 -> R.string.cube_444
            TYPE_555 -> R.string.cube_555
            TYPE_666 -> R.string.cube_666
            TYPE_777 -> R.string.cube_777
            TYPE_CLOCK -> R.string.cube_clock
            TYPE_MEGA -> R.string.cube_mega
            TYPE_PYRA -> R.string.cube_pyra
            TYPE_SKEWB -> R.string.cube_skewb
            TYPE_SQUARE1 -> R.string.cube_sq1
            else -> R.string.cube_333
        }
    }

    /**
     * Converts a duration value in milliseconds to a String
     * @param time
     * the time in milliseconds
     * @param timeFormat
     * the format. see FORMAT constants in [PuzzleUtils]
     * @return
     * a String containing the converted time
     */
    @JvmStatic
    fun convertTimeToString(time: Long, timeFormat: Int): String {
        if (time == TIME_DNF) return "DNF"
        if (time == 0L) return "--"

        val secondsTotal = time / 1000
        val millis = (time % 1000) / 10
        val seconds = secondsTotal % 60
        val minutesTotal = secondsTotal / 60
        val minutes = minutesTotal % 60
        val hours = minutesTotal / 60

        val formattedString = StringBuilder()

        if (timeFormat == FORMAT_LARGE) {
            if (hours > 0) {
                formattedString.append(hours).append("h ")
            }
            formattedString.append(minutes).append("m")
        } else {
            if (hours > 0) {
                formattedString.append(hours).append("h ")
            }
            if (minutes > 0 || hours > 0) {
                if (hours > 0) {
                    formattedString.append(String.format(Locale.getDefault(), "%02d", minutes))
                } else {
                    formattedString.append(minutes)
                }
                formattedString.append(":")
            }

            if (minutes > 0 || hours > 0) {
                formattedString.append(String.format(Locale.getDefault(), "%02d", seconds))
            } else {
                if (time < 10000) {
                    formattedString.append(seconds)
                } else {
                    formattedString.append(String.format(Locale.getDefault(), "%02d", seconds))
                }
            }
        }

        // Append millis
        when (timeFormat) {
            FORMAT_DEFAULT -> {
                formattedString.append(".")
                formattedString.append(String.format(Locale.getDefault(), "%02d", millis))
            }
            FORMAT_SMALL_MILLI -> {
                formattedString.append("<small>.")
                formattedString.append(String.format(Locale.getDefault(), "%02d", millis))
                formattedString.append("</small>")
            }
            FORMAT_NO_MILLI -> {}
            else -> {}
        }
        return formattedString.toString()
    }

    /**
     * Converts times in the format "M:SS.s", or "S.s" into an integer number of milliseconds. The
     * minutes value may be padded with zeros. The "ss" is the fractional number of seconds and may
     * be given to any desired precision, but will be rounded to a whole number of milliseconds.
     * For example, "1:23.45" is parsed to 83,450 ms and "95.6789" is parsed to 95,679 ms. Where
     * minutes are present, the seconds can be padded with zeros or not, but the number of seconds
     * must be less than 60, or the time will be treated as invalid. Where minutes are not present,
     * the number of seconds can be 60 or greater.
     *
     * @param time
     * The time string to be parsed. Leading and trailing whitespace will be trimmed before
     * the time is parsed. If minus signs are present, the result is not defined.
     *
     * @return
     * The parsed time in milliseconds. The value is rounded to the nearest multiple of 10
     * milliseconds. If the time cannot be parsed because the format does not conform to the
     * requirements, zero is returned.
     */
    @JvmStatic
    fun parseTime(time: String): Int {
        val timeStr = time.trim { it <= ' ' }
        val colonIdx = timeStr.indexOf(':')
        var parsedTime = 0
        try {
            if (colonIdx != -1) {
                if (colonIdx > 0 && colonIdx < timeStr.length) {
                    // At least one digit for the minutes, so still a valid time format. Format is
                    // expected to be "M:S.s" (zero padding to "MM" and "SS" is optional).
                    val minutes = timeStr.substring(0, colonIdx).toInt()
                    val seconds = timeStr.substring(colonIdx + 1).toFloat()
                    if (seconds < 60f) {
                        parsedTime += 60000 * minutes + (seconds * 1000f).roundToInt()
                    } // else "parsedTime" remains zero as seconds value is out of range (>= 60).
                } // else "parsedTime" remains zero as there is nothing before or after the colon.
            } else {
                // Format is expected to be "S.s", with arbitrary precision and padding.
                parsedTime = (timeStr.toFloat() * 1000f).roundToInt()
            }
        } catch (_: NumberFormatException) {
            parsedTime = 0 // Invalid time format.
        }
        return 10 * ((parsedTime + 5) / 10)
    }

    /**
     * Parses a time in the format hh'h'mm:ss.SS and returns it in milliseconds
     * @param time the string to be parsed
     * @return the time in milliseconds
     */
    @JvmStatic
    fun parseAddedTime(time: String): Long {
        var timeMillis: Long = 0
        val times = time.split("[h:.]".toRegex()).toTypedArray()
        when (times.size) {
            2 ->                 // ss.SS
                timeMillis += times[0].toLong() * 1000 + times[1].toLong() * 10
            3 ->                 // mm:ss.SS
                timeMillis += times[0].toLong() * 60000 + times[1].toLong() * 1000 + times[2].toLong() * 10
            4 ->                 // hh'h'mm:ss.SS
                timeMillis += times[0].toLong() * 3600000 + times[1].toLong() * 60000 + times[2].toLong() * 1000 + times[3].toLong() * 10
        }
        return timeMillis
    }

    /**
     * Applies a penalty to a solve
     *
     * @param solve   A [Solve]
     * @param penalty The penalty (refer to static constants on top)
     *
     * @return The solve with the penalty applied
     */
    @JvmStatic
    fun applyPenalty(solve: Solve, penalty: Int): Solve {
        when (penalty) {
            PENALTY_DNF -> {
                if (solve.penalty == PENALTY_PLUSTWO) solve.time -= 2000
                solve.penalty = PENALTY_DNF
            }
            PENALTY_PLUSTWO -> {
                if (solve.penalty != PENALTY_PLUSTWO) solve.time += 2000
                solve.penalty = PENALTY_PLUSTWO
            }
            NO_PENALTY -> {
                if (solve.penalty == PENALTY_PLUSTWO) solve.time -= 2000
                solve.penalty = NO_PENALTY
            }
        }
        return solve
    }

    /**
     * Formats the details of the most recent average-of-N calculation for times recorded in the
     * current session. The string shows the average value and the list of times that contributed
     * to the calculation of that average. If the average calculation requires the elimination of
     * the best and worst times, these times are shown in parentheses.
     *
     * @param n     The value of "N" for which the "average-of-N" is required.
     * @param stats The statistics from which to get the details of the average calculation.
     *
     * @return
     * The average-of-N in string format; or `null` if there is no average calculated for
     * that value of "N", or if insufficient (less than "N") times have been recorded in the
     * current session, of if `stats` is `null`.
     */
    private fun formatAverageOfN(n: Int, stats: Statistics?): String? {
        val averageOf = stats?.getAverageOf(n, true)
        if (stats == null || averageOf == null) {
            return null
        }
        val aoN = averageOf.averageOfN
        val times = aoN.times
        val average = aoN.average
        if (average == AverageCalculator.UNKNOWN || times == null) {
            return null
        }
        val s = StringBuilder(convertTimeToString(tr(average), FORMAT_DEFAULT))
        s.append(" = ")
        for (i in 0 until n) {
            val time = convertTimeToString(tr(times[i]), FORMAT_DEFAULT)

            // The best and worst indices may be -1, but that is OK: they just will not be marked.
            if (i == aoN.bestTimeIndex || i == aoN.worstTimeIndex) {
                s.append('(').append(time).append(')')
            } else {
                s.append(time)
            }
            if (i < n - 1) {
                s.append(", ")
            }
        }
        return s.toString()
    }

    private fun replaceAll(str: String, map: HashMap<String, String>): String {
        val rotated = StringBuilder()
        var move: Char
        for (turn in str.split(" ".toRegex()).toTypedArray()) {
            if (turn.isEmpty()) continue
            // If a turn is a prime move, get only the first char (F' becomes F)
            move = turn[0]
            if (map.containsKey(move.toString())) {
                rotated.append(map[move.toString()]).append(if (turn.length > 1) turn[1].toString() + " " else " ")
            } else {
                rotated.append(turn).append(" ")
            }
        }
        return rotated.toString()
    }

    // returns new string with transformed algorithm.
    // Returnes sequence of moves that get the cube to the same position as (alg + rot) does, but without cube rotations.
    // Example: applyRotationForAlgorithm("R U R'", "y") = "F U F'"
    @JvmStatic
    fun applyRotationForAlgorithm(alg: String, rot: String): String {
        val map: HashMap<String, String>
        when (rot) {
            "y" -> map = object : HashMap<String, String>() {
                init {
                    put("R", "F")
                    put("F", "L")
                    put("L", "B")
                    put("B", "R")
                }
            }
            "y'" -> map = object : HashMap<String, String>() {
                init {
                    put("R", "B")
                    put("B", "L")
                    put("L", "F")
                    put("F", "R")
                }
            }
            "y2" -> map = object : HashMap<String, String>() {
                init {
                    put("R", "L")
                    put("L", "R")
                    put("B", "F")
                    put("F", "B")
                }
            }
            else -> return alg
        }
        return replaceAll(alg, map)
    }

    /**
     * Shares an average-of-N, formatted to a simple string.
     *
     * @param n
     * The value of "N" for which the average is required.
     * @param puzzleType
     * The name of the type of puzzle being shared.
     * @param stats
     * The statistics that contain the required details about the average.
     * @param activityContext
     * An activity context required to start the sharing activity. An application context is
     * not appropriate, as using it may disrupt the task stack.
     *
     * @return
     * `true` if it is possible to share the average; or `false` if it is not.
     */
    @JvmStatic
    fun shareAverageOf(
        n: Int, puzzleType: String?, stats: Statistics?, activityContext: Activity
    ): Boolean {
        val averageOfN = formatAverageOfN(n, stats)
        if (averageOfN != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                activityContext.getString(getPuzzleName(puzzleType))
                        + ": " + formatAverageOfN(n, stats)
            )
            shareIntent.type = "text/plain"
            activityContext.startActivity(shareIntent)
            return true
        }
        Toast.makeText(activityContext, R.string.fab_share_error, Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * Creates a histogram of the frequencies of solve times for the current session. Times are
     * truncated to whole seconds.
     *
     * @param stats The statistics from which to get the frequencies.
     *
     * @return
     * A multi-line string presenting the histogram using "ASCII art"; or an empty string if
     * the statistics are `null`, or if no times have been recorded.
     */
    @JvmStatic
    fun createHistogramOf(stats: Statistics?): String {
        val histogram = StringBuilder(1000)
        if (stats != null) {
            val timeFreqs = stats.sessionTimeFrequencies

            // Iteration order starts with DNF and then goes by increasing time.
            for (time in timeFreqs.keys) {
                histogram
                    .append('\n')
                    .append(convertTimeToString(tr(time), FORMAT_NO_MILLI))
                    .append(": ")
                    .append(convertToBars(timeFreqs[time]!!)) // frequency value.
            }
        }
        return histogram.toString()
    }

    /**
     * Shares a histogram showing the frequency of solve times falling into intervals of one
     * second. Only times for the current session are presented in the histogram.
     *
     * @param puzzleType
     * The name of the type of puzzle being shared.
     * @param stats
     * The statistics that contain the required details to present the histogram.
     * @param activityContext
     * An activity context required to start the sharing activity. An application context is
     * not appropriate, as using it may disrupt the task stack.
     *
     * @return
     * `true` if it is possible to share the histogram; or `false` if it is not.
     */
    @JvmStatic
    fun shareHistogramOf(
        puzzleType: String?, stats: Statistics?, activityContext: Activity
    ): Boolean {
        val solveCount = stats?.sessionNumSolves ?: 0 // Includes DNFs.
        if (solveCount > 0) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                activityContext.getString(
                    R.string.fab_share_histogram_solvecount,
                    activityContext.getString(getPuzzleName(puzzleType)), solveCount
                ) + ":" +
                        createHistogramOf(stats)
            )
            shareIntent.type = "text/plain"
            activityContext.startActivity(shareIntent)
            return true
        }
        return false
    }

    /**
     * Takes an int N and converts it to bars █. Used for histograms
     *
     * @param n
     * @return
     */
    private fun convertToBars(n: Int): String {
        val temp = StringBuilder()
        repeat(n) {
            temp.append("█")
        }
        return temp.toString()
    }
}
