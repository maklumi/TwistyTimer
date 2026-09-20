package com.aricneto.twistytimer.stats

import android.graphics.Color
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * A collector for solve times and related statistics (average times) to be presented in a chart.
 * 
 * @author damo
 */
class ChartStatistics private constructor(
    statistics: Statistics, isForCurrentSessionOnly: Boolean,
    chartStyle: ChartStyle
) {
    /**
     * The collection of statistics that are required to support the calculation of any number of
     * average-of-N lines in the graph.
     */
    val statistics: Statistics = statistics

    /**
     * The styles that will be applied to the data sets of the chart.
     */
    private val mChartStyle: ChartStyle

    /**
     * The values of "N" for all "average-of-N" data sets to be charted.
     */
    private val mNsOfAverages: IntArray

    /**
     * Indicates if all the charted times required are across the current session only. If
     * only times for the current session are required, a more efficient approach may be taken to
     * load the saved solve times.
     * 
     * @return
     * `true` if all required chart data applies only to solve times for the current
     * session; or `false` if the data includes times across all past and current
     * sessions.
     */
    /**
     * Indicates if the chart data is for the current session only or for all past and current
     * sessions.
     */
    val isForCurrentSessionOnly: Boolean// Perhaps adjust this for the number of data points in the chart data.

    /**
     * Gets whether the chart should draw discrete circles for the datapoints
     * 
     * @return True if it should draw circles
     */
    /**
     * Indicates if the main dataset should be represented as discrete points or a continuous line
     */
    private val drawCircle: Boolean

    /**
     * The chart data for all solves and for each "average-of-N". The first data set (index zero)
     * is the data set for all solves. The other data sets correspond to the data sets for each
     * average-of-N, starting at index one and in the order of the entries in the
     * [.mNsOfAverages] array (i.e., the entry at index zero of that array is the value of
     * "N" for the average values in the data set at index one in the chart data).
     */
    // At present, a line chart is shown, but it could be changed to show a mix of different types
    // of charts in the future, so the field is "mChartData", not "mLineData".
    private var mChartData: LineData? = null

    /**
     * The current X-index for the solve time added to the chart.
     */
    private var mXIndex = 0

    /**
     * The current best solve time recorded so far (in milliseconds).
     */
    private var mBestTime: Long = 0

    /**
     * The "pre-compiled" date formatter for the X-axis labels.
     */
    private val mXValueFormatter: DateTimeFormatter?

    /**
     * The day for which the previous data set entry was recorded. If the day has not changed, the
     * value of [.mPrevEntryXValue] can be re-used instead of re-formatting the date object
     * to a new string. If `null`, there was no previous entry.
     */
    private var mPrevEntryDay: LocalDate? = null

    /**
     * The formatted X-value with which the previous data set entry was recorded. If the day has
     * not changed (tested against [.mPrevEntryDay]), this X-value can be re-used instead of
     * re-formatting the date object to a new string. If `null`, there was no previous entry.
     */
    private var mPrevEntryXValue: String? = null

    /**
     * Creates a new collector for chart statistics that will chart all collected values and all
     * averages-of-N values collected by the given `Statistics`. Each instance of
     * `ChartStatistics` can collect statistics for the set of solve times for the current
     * session, or the set of solve times for all sessions, but not a combination of the two.
     * 
     * @param statistics
     * The statistics that will be updated as each solve time is recorded and that will provide
     * the average values to be charted. Must not be `null`. Regardless of whether
     * the chart data is for the current session or for all sessions, the statistics must be
     * configured to collect only solve times for the current session (i.e.,
     * [Statistics.isForCurrentSessionOnly] must return `true`).
     * @param isForCurrentSessionOnly
     * `true` if the solve times to be charted are only those solves added in the current
     * sessions; or `false` if the solve times are only those solves added across all
     * sessions.
     * @param chartStyle
     * The styling information for the chart. This defines the labels and colors for the data
     * sets, among other information.
     * 
     * @throws IllegalArgumentException
     * If `statistics` is not configured for the current session only.
     * @throws IllegalStateException
     * If there are more than three average-of-N lines to be graphed.
     */
    init {
        require(statistics.isForCurrentSessionOnly) { "Statistics must be for current session only." }

        mChartStyle = chartStyle
        mNsOfAverages = statistics.nsOfAverages
        this.isForCurrentSessionOnly = isForCurrentSessionOnly
        this.drawCircle = getBoolean(R.string.pk_stat_discrete_graph_dataset, false)

        /*/ Unfortunately, the mean value can only be set in the "LimitLine" constructor, so save
        // the label and color of the line now (while a "Context" is available) and create the line
        // later in "applyTo".
        mLimitLineLabel = chartStyle.getLimitLineLabel();
        mLimitLineColor = chartStyle.getLimitLineColor();
        */

        // Set the formatter for the date label on the chart X-axis. Localize the format, mostly to
        // support the "MM/DD" order used in the USA. It will fall back to the most common "DD/MM"
        // format (from "values/formats.xml") if no more specific localized format is found (such
        // as in "values-en-rUS/formats.xml". This also "pre-compiles" the pattern, making the
        // formatting operation faster later.
        mXValueFormatter = DateTimeFormatter.ofPattern(chartStyle.dateFormatSpec)

        // Initialize and reset everything to a sane, empty state.
        reset()
    }

    /**
     * Resets all chart data and statistics to their initial, empty state.
     */
    fun reset() {
        statistics.reset()
        mXIndex = 0
        mBestTime = Long.MAX_VALUE
        mPrevEntryDay = null
        mPrevEntryXValue = null

        // There does not seem to be an easy way to clear existing Y-values *and* X-values from
        // each data set in the chart data. Just create a new one instead.
        mChartData = LineData()

        // The order in which the data sets are added is important to ensure that "DS_ALL", etc.
        // remain meaningful.
        addMainDataSets(
            mChartData!!, mChartStyle.allTimesLabel, mChartStyle.allTimesColor,
            mChartStyle.bestTimesLabel, mChartStyle.bestTimesColor
        )

        for (nIndex in mNsOfAverages.indices) {
            addAoNDataSets(
                mChartData!!,
                mChartStyle.averageOfNLabelPrefix + mNsOfAverages[nIndex],
                mChartStyle.getExtraColor(nIndex)
            )
        }
    }

    /**
     * Adds the main data set for all times and the data set for the progression of record best
     * times among all times. The progression of best times are marked in a different color to the
     * main line of all time using circles lined with a dashed line. This will appear to connect
     * the lowest troughs along the main line of all times.
     * 
     * @param chartData The chart data to which to add the new data sets.
     * @param allLabel  The label of the all-times line.
     * @param allColor  The color of the all-times line.
     * @param bestLabel The label of the best-times line.
     * @param bestColor The color of the best-times line.
     */
    private fun addMainDataSets(
        chartData: LineData, allLabel: String?, allColor: Int,
        bestLabel: String?, bestColor: Int
    ) {
        // Main data set for all solve times.
        val mainDataSet = createDataSet(allLabel, allColor)

        mainDataSet.setDrawCircles(this.drawCircle)
        mainDataSet.setCircleRadius(this.circleRadius)
        mainDataSet.setCircleColor(allColor)
        mainDataSet.setColor(getLineColor(allColor))

        // Enhancement: Gradient fill
        mainDataSet.setDrawFilled(true)
        mainDataSet.fillAlpha = 60

        chartData.addDataSet(mainDataSet)

        // Data set to show the progression of best times along the main line of all times.
        val bestDataSet = createDataSet(bestLabel, bestColor)

        bestDataSet.enableDashedLine(3f, 6f, 0f)

        bestDataSet.setDrawCircles(true)
        bestDataSet.circleRadius = BEST_TIME_CIRCLE_RADIUS_DP
        bestDataSet.setCircleColor(bestColor)

        bestDataSet.setDrawValues(false)
        bestDataSet.setValueTextColor(bestColor)
        bestDataSet.valueTextSize = BEST_TIME_VALUES_TEXT_SIZE_DP
        bestDataSet.valueFormatter = TimeChartValueFormatter()

        chartData.addDataSet(bestDataSet)
    }

    /**
     * Adds the data set for the average-of-N (AoN) times and the corresponding data set for the
     * single best average time for that value of "N". The best AoN times are not shown as a
     * progression; only one time is shown, and it superimposed on its main AoN line, rendered in
     * the same color as a circle and with the value drawn on the chart.
     * 
     * @param chartData The chart data to which to add the new data sets.
     * @param label     The label of the AoN line and best AoN time marker.
     * @param color     The color of the AoN line and best AoN time marker.
     */
    private fun addAoNDataSets(chartData: LineData, label: String?, color: Int) {
        // Main AoN data set for all AoN times for one value of "N".
        chartData.addDataSet(createDataSet(label, color))

        // Data set for the single best AoN time for this "N".
        val bestAoNDataSet = createDataSet(label, color)

        bestAoNDataSet.setDrawCircles(true)
        bestAoNDataSet.circleRadius = BEST_TIME_CIRCLE_RADIUS_DP
        bestAoNDataSet.setCircleColor(color)

        // Drawing the value of the best AoN time for each "N" seems like it would be a good idea.
        // But the values are really hard because they appear over other chart lines and sometimes
        // over the values drawn for the best time progression. Disabling them is no great loss,
        // as the statistics table shows the same values, anyway. Just showing a circle to mark
        // the best AoN time looks well enough on its own.
        bestAoNDataSet.setDrawValues(false)


        chartData.addDataSet(bestAoNDataSet)
    }

    /**
     * Creates a data set with the given label and color. Highlights and drawing of values and
     * circles are disabled, as that is common for many cases.
     * 
     * @param label The label to assign to the new data set.
     * @param color The line color to set for the new data set.
     */
    private fun createDataSet(label: String?, color: Int): LineDataSet {
        // A legend is enabled on the chart view in the graph fragment. The legend is created
        // automatically, but requires a unique labels and colors on each data set.
        val dataSet = LineDataSet(null, label)

        // Enhancement: Cubic lines for a more modern look
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.15f

        // A dashed line can make peaks inaccurate. It also makes the graph look too "busy". It
        // is OK for some uses, such as progressions of best times, but that is left to the caller
        // to change once this new data set is returned.
        //
        // If graphing only times for a session, there will be fewer, and a thicker line will look
        // well. However, if all times are graphed, a thinner line will probably look better, as
        // the finer details will be more visible.
        //
        // Also, the library specifies that a thicker line has increased performance use compared
        // thinner lines, so don't make lines too thick if the dataset is large!
        dataSet.setLineWidth(this.lineWidth)
        dataSet.setColor(color)
        dataSet.isHighlightEnabled = false

        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)

        return dataSet
    }

    /**
     * Applies the data sets for the collected chart statistics to the given chart and sets the
     * appropriate legend.
     * 
     * @param chart The chart to which to apply the collected statistics.
     */
    @Throws(IllegalStateException::class)
    fun applyTo(chart: LineChart) {
        // It seems that it is important to set the custom legend before setting the chart data.
        // If it is done the other way around, some cached values related to the layout of the
        // legend for the previous statistics are not updated to match the new data sets and
        // crashes occur during rendering of the legend.
        configureLegend(chart.legend)

        // One confusing aspect is that the visible count that the chart renderer compares to this
        // maximum count includes all points from all data sets, even those that have not been set
        // to show values (i.e., even when "setDrawValues(false)" is applied). For example, if
        // there are 1,000 solve times in one data set and then 951 Ao50 times and 901 Ao100 times,
        // and 8 "best" times, then the total number of visible data points is 2,860, even though
        // the chart is only 1,000 points wide and even though only the 8 "best" times will show
        // their values. Therefore, the maximum count needs to be about 3 times higher than the
        // number of solve times that would give rise to the number of "best" times that could have
        // their values shown without much visual overlap.
        chart.setMaxVisibleValueCount(2000)

        // Use a custom renderer to draw the values of the best times *below* their data points.
        //chart.setRenderer(new OffsetValuesLineChartRenderer(chart, BEST_TIME_VALUES_Y_OFFSET_DP));
        chart.setData(mChartData)
    }

    /**
     * Configures the given `Legend` for the data sets that will be displayed by the chart.
     * 
     * @param legend The legend to be configured.
     */
    private fun configureLegend(legend: Legend) {
        // NOTE: If "Legend" is allowed to configure itself automatically, it will add two entries
        // for each AoN/best-AoN pair of data sets, but only one should be shown. Go custom....
        val numNs = mNsOfAverages.size
        val legendEntries = arrayOfNulls<LegendEntry>(DS_AVG_0 + numNs)

        var ds: LineDataSet?

        ds = mChartData!!.getDataSetByIndex(DS_ALL) as LineDataSet?
        if (ds == null) return

        legendEntries[DS_ALL] = LegendEntry()
        legendEntries[DS_ALL]!!.form = Legend.LegendForm.CIRCLE
        legendEntries[DS_ALL]!!.label = ds.label
        legendEntries[DS_ALL]!!.formColor = mChartStyle.allTimesColor

        ds = mChartData!!.getDataSetByIndex(DS_BEST) as LineDataSet?
        if (ds == null) return

        legendEntries[DS_BEST] = LegendEntry()
        legendEntries[DS_BEST]!!.form = Legend.LegendForm.CIRCLE
        legendEntries[DS_BEST]!!.label = ds.label
        legendEntries[DS_BEST]!!.formColor = ds.color

        for (nIndex in 0..<numNs) {
            // A main AoN data set. The "best AoN" data sets are not represented in the legend.
            ds = mChartData!!.getDataSetByIndex(DS_AVG_0 + 2 * nIndex) as LineDataSet?
            legendEntries[DS_AVG_0 + nIndex] = LegendEntry()
            legendEntries[DS_AVG_0 + nIndex]!!.form = Legend.LegendForm.CIRCLE
            legendEntries[DS_AVG_0 + nIndex]!!.label = ds!!.label
            legendEntries[DS_AVG_0 + nIndex]!!.formColor = ds.color
        }

        legend.setCustom(legendEntries)
    }

    /**
     * Records a solve time. The time value should be in milliseconds. If the solve is a DNF,
     * call [.addDNF] instead.
     * 
     * @param time
     * The solve time in milliseconds. Must be positive (though [AverageCalculator.DNF]
     * is also accepted).
     * @param date
     * The date on which the solve time was recorded. The values should be in milliseconds
     * since the Unix epoch time.
     * 
     * @throws IllegalArgumentException
     * If the time is not greater than zero and is not `DNF`.
     */
    fun addTime(time: Long, date: Long) {
        var isEntryAdded = false

        // The value of "time" is validated by "Statistics.addTime".
        statistics.addTime(time, true) // May throw IAE.

        if (time != AverageCalculator.DNF) {
            mChartData!!.addEntry(Entry(mXIndex.toFloat(), time / 1000f), DS_ALL)
            isEntryAdded = true

            // Only update the recorded best time if it changes. The result should be a line that
            // traces (if lucky) a staircase descending from left to right (never rising).
            if (time < mBestTime) {
                mBestTime = time
                mChartData!!.addEntry(Entry(mXIndex.toFloat(), mBestTime / 1000f), DS_BEST)
            }
        }

        for (nIndex in mNsOfAverages.indices) {
            val ac = statistics.getAverageOf(mNsOfAverages[nIndex], true)
            val averageTime = ac!!.currentAverage

            if (averageTime != AverageCalculator.DNF && averageTime != AverageCalculator.UNKNOWN) {
                // AoN data sets start at "DS_AVG_0" and come in pairs. In each pair, the first is
                // the data set for all AoN times for that "N" and the second is the data set for
                // the single best AoN time for that "N".
                val aonDSIndex: Int = DS_AVG_0 + 2 * nIndex
                val aonYValue = averageTime / 1000f

                mChartData!!.addEntry(Entry(mXIndex.toFloat(), aonYValue), aonDSIndex)
                isEntryAdded = true

                // Just keep a single entry in each data set for each best AoN; it will be rendered
                // as a single circle that is coincident with the main AoN line and its value will
                // be drawn. There is no line charting the *progression* of best AoN times.
                val bestAoNDS = mChartData!!.getDataSetByIndex(aonDSIndex + 1) as LineDataSet

                if (bestAoNDS.entryCount > 0) { // Should be 0 or 1, nothing more.
                    val oldEntry = bestAoNDS.getEntryForIndex(0) // Not an X-index.

                    if (aonYValue < oldEntry.y.toFloat()) {
                        // A new best AoN time! Replace the old one with this new one.
                        bestAoNDS.removeEntry(oldEntry)
                        bestAoNDS.addEntry(Entry(mXIndex.toFloat(), aonYValue))
                    }
                } else {
                    // This is the first AoN time, so just add it as the best (and only) AoN time.
                    bestAoNDS.addEntry(Entry(mXIndex.toFloat(), aonYValue))
                }
            }
        }

        if (isEntryAdded) {
            /** The nature of the data means that sequential times will often be from the same
            * / session performed on the same day. Therefore, it is easy to optimize this a bit by
            * / not formatting the same day over-and-over. This may also save memory, as only a
            * / single "String" instance is created for each day. */
             mXIndex++
        }
        // If the new solve and all current averages were DNF or UNKNOWN, then no entry was added
        // to the chart, so do not add any X-axis value and do not increment the X-index.
    }

    fun getAllSolvePoints(): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val ds = mChartData?.getDataSetByIndex(DS_ALL) ?: return points
        for (i in 0 until ds.entryCount) {
            val entry = ds.getEntryForIndex(i)
            points.add(entry.x to entry.y)
        }
        return points
    }

    fun getBestSolvePoints(): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val ds = mChartData?.getDataSetByIndex(DS_BEST) ?: return points
        for (i in 0 until ds.entryCount) {
            val entry = ds.getEntryForIndex(i)
            points.add(entry.x to entry.y)
        }
        return points
    }

    fun getAveragePoints(n: Int): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val nIndex = statistics.nsOfAverages.indexOf(n)
        if (nIndex == -1) return points
        val dsIndex = DS_AVG_0 + 2 * nIndex
        val ds = mChartData?.getDataSetByIndex(dsIndex) ?: return points
        for (i in 0 until ds.entryCount) {
            val entry = ds.getEntryForIndex(i)
            points.add(entry.x to entry.y)
        }
        return points
    }

    /**
     * Records a did-not-finish (DNF) solve, one where no time was recorded.
     * 
     * @param date
     * The date on which the solve time was recorded. The values should be in milliseconds
     * since the Unix epoch time.
     */
    // This method takes away any confusion about what time value represents a DNF.
    fun addDNF(date: Long) {
        addTime(AverageCalculator.DNF, date)
    }

    private val lineWidth: Float
        /**
         * Gets the width to use for all lines on the chart. The lines are shown slightly wider when
         * only the session times are displayed, as there will be less data points on the chart.
         * 
         * @return The line width (in DIP units).
         */
        get() =// Perhaps adjust this for the number of data points in the chart data.
            if (this.isForCurrentSessionOnly) LINE_WIDTH_THICK_DP else LINE_WIDTH_THIN_DP

    private val circleRadius: Float
        /**
         * Gets the circle radius for the main line on the chart. The circles are shown slightly smaller
         * when only the session times are displayed, as there will be less data points on the chart.
         * 
         * @return The circle radius (in DIP units).
         */
        get() =// Perhaps adjust this for the number of data points in the chart data.
            if (this.isForCurrentSessionOnly) MAIN_TIME_CIRCLE_RADIUS_DP_BIG else MAIN_TIME_CIRCLE_RADIUS_DP_SMALL

    /**
     * Gets the color to use for the main data line in the chart.
     * 
     * @return The color
     */
    private fun getLineColor(defaultColor: Int): Int {
        // Perhaps adjust this for the number of data points in the chart data.
        return if (this.drawCircle) Color.TRANSPARENT else defaultColor
    }

    /**
     * A formatter for time values displayed beside points in the chart. This converts the stored
     * values (in seconds) to the normal representation.
     */
    private class TimeChartValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            // "value" is in fractional seconds. Convert to whole milliseconds and format it.
            return convertTimeToString(
                (value * 1000).roundToInt().toLong(),
                PuzzleUtils.FORMAT_DEFAULT
            )
        }
    }

    companion object {
        // NOTE: This "ChartStatistics" class does not extend "Statistics", it contains an instance of
        // that class. The API of "Statistics" is not compatible, as it is one-dimensional (requiring
        // only a solve times to be added), but "ChartStatistics" is two-dimensional (requiring both
        // solve times and the date of each of the solve events). Re-use by containment avoids the mess
        // of trying to hide "Statistics.addTime", "Statistics.addDNF" and various other methods.
        // NOTE: "ChartStatistics" is expected to be used from a Loader or AsyncTask, so it is
        // preferable not to have this class depend on a Context, as that could lead to memory leaks.
        // Instead, "ChartStyle" captures the necessary values from resources and theme attributes via
        // a Context, and then it can be passed when creating an instance of this class. Neither class
        // then needs to hold a Context. "ChartStyle" can be created before the Loader or AsyncTask is
        // invoked and passed in before execution.
        /**
         * The line width to use in the chart when a thicker line is appropriate. The value is in DIP
         * units.
         */
        private const val LINE_WIDTH_THICK_DP = 1.8f

        /**
         * The line width to use in the chart when a thinner line is appropriate. The value is in DIP
         * units.
         */
        private const val LINE_WIDTH_THIN_DP = 1.0f

        /**
         * The text size to use for limit line marking the mean time. The value is in DIP units.
         */
        private const val MEAN_LIMIT_LINE_TEXT_SIZE_DP = 12f

        /**
         * The text size to use for the value text shown near the data points for the best times. The
         * value is in DIP units.
         */
        private const val BEST_TIME_VALUES_TEXT_SIZE_DP = 10f

        /**
         * The circle radius to use for the circles drawn at the data points for the "best" times. The
         * value is in DIP units.
         */
        private const val BEST_TIME_CIRCLE_RADIUS_DP = 3.5f

        /**
         * The circle radius to use in the chart when a smaller circle is appropriate. The value is in
         * DIP units.
         */
        private const val MAIN_TIME_CIRCLE_RADIUS_DP_SMALL = 1f

        /**
         * The circle radius to use in the chart when a bigger circle is appropriate. The value is in
         * DIP units.
         */
        private const val MAIN_TIME_CIRCLE_RADIUS_DP_BIG = 1.5f

        /**
         * The data set index for the graph of all solve times.
         */
        private const val DS_ALL = 0

        /**
         * The data set index for the graph of changes to the best solve time.
         */
        private const val DS_BEST = 1

        /**
         * The data set index for the first of a series of graphs of "average-of-N" (AoN) solve times.
         * The data set at this index corresponds to the AoN for the value of "N" at index zero in
         * [.mNsOfAverages]. Like the [.DS_ALL] and [.DS_BEST] indices, these AoN
         * indices come in pairs, with the first index for the data set of AoN times and the second for
         * the best AoN time for that "N". The data set at `DS_AVG_0 + 2` corresponds to the
         * average for the value of "N" at index one in `mNsOfAverages`, and so on.
         */
        private const val DS_AVG_0 = 2

        /**
         * Creates a new collector for chart data and statistics for the all-time chart. This includes
         * data for all solve times across all past and current sessions and the running averages of 50
         * and 100 consecutive times. These averages permit all but one solve to be a DNF solve.
         *
         * @param chartStyle
         * The chart style information required for the data sets that will be populated with
         * statistics.
         *
         * @return
         * The collector for chart statistics.
         */
        fun newAllTimeChartStatistics(chartStyle: ChartStyle): ChartStatistics {
            return ChartStatistics(
                Statistics.newAllTimeAveragesChartStatistics(), false, chartStyle
            )
        }

        /**
         * Creates a new collector for chart data and statistics for the current session chart. This
         * includes data for all solve times across only the current session and the running averages
         * of 5 and 12 consecutive times. These averages permit no more than one solve to be a DNF
         * solve.
         *
         * @param chartStyle
         * The chart style information required for the data sets that will be populated with
         * statistics.
         *
         * @return
         * The collector for chart statistics.
         */
        fun newCurrentSessionChartStatistics(chartStyle: ChartStyle): ChartStatistics {
            return ChartStatistics(
                Statistics.newCurrentSessionAveragesChartStatistics(), true, chartStyle
            )
        }
    }
}
