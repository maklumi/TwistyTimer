package com.aricneto.twistytimer.stats

import android.content.Context
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor

/**
 * A collection of styling information for a chart. This is initialized from string resources and
 * theme attributes and requires an activity context to access the theme attributes. However, if
 * [ChartStatistics] are loaded in the background, an activity context may not be available
 * (and it may not be safe to attempt to make one available outside the main UI thread).
 * Therefore, create this styling information with an activity context and then use it to
 * instantiate any required `ChartStatistics` instances.
 * 
 * @author damo
 */
class ChartStyle(context: Context) {
    /**
     * Gets the color to use when representing the data set for all times for all sessions, or all
     * times for the current session.
     * 
     * @return The color for all times.
     */
    /**
     * The color to use for the main data set of the chart that displays all times for all sessions,
     * or all times for the current session.
     */
    val allTimesColor: Int

    /**
     * Gets the color to use when representing the data set for the progression of best times.
     * 
     * @return The color for the progression of best times.
     */
    /**
     * The color to use for the data set of the chart that display the progression of best times.
     */
    val bestTimesColor: Int

    /**
     * Gets the color to use for a limit line displayed on the chart.
     * 
     * @return The color for a limit line.
     */
    /**
     * The color to use for a limit line shown on the chart.
     */
    val limitLineColor: Int

    /**
     * The colors to use for the "extra" data sets of the chart. These are likely to be average-of-N
     * data sets.
     */
    // NOTE: An array makes it easier to support new "extra" color attributes to support charts
    // with more average-of-N, or other, data sets.
    private val mExtraColors = IntArray(MAX_EXTRA_COLORS)

    /**
     * Gets the label to use when representing the data set for all times for all sessions, or all
     * times for the current session.
     * 
     * @return The label for all times.
     */
    /**
     * The label to use for the main data set of the chart that displays all times for all sessions,
     * or all times for the current session.
     */
    val allTimesLabel: String

    /**
     * Gets the label to use when representing the data set for the progression of best times.
     * 
     * @return The label for the progression of best times.
     */
    /**
     * The label to use for the data set of the chart that display the progression of best times.
     */
    val bestTimesLabel: String

    /**
     * Gets the label prefix to use when representing a data set for average-of-N times. The value
     * of "N" should be appended to the prefix to form the full label.
     * 
     * @return The label prefix for average-of-N data sets.
     */
    /**
     * The label prefix to use for the average-of-N data sets of the chart. The value of "N" is
     * appended to the label prefix. For example, the prefix may be "Ao" and the value 12 may be
     * appended to form the label "Ao12" for the average-of-12 data set.
     */
    val averageOfNLabelPrefix: String

    /**
     * Gets the label to use for a limit line displayed on the chart.
     * 
     * @return The label for a limit line.
     */
    /**
     * The label to use for a limit line shown on the chart.
     */
    val limitLineLabel: String

    /**
     * Gets the date format specification to use when formatting the date value shown along the
     * X-axis of the chart.
     * 
     * @return The date format specification.
     */
    /**
     * The data format specification for the X-axis labels on the chart.
     */
    val dateFormatSpec: String

    /**
     * Creates a new chart style with label and color values. The labels are loaded from string
     * resources and the colors from theme attributes.
     * 
     * @param context
     * The context required to access the string resources and the line colors defined for the
     * current theme. An application context is not sufficient to access the theme colors, so
     * an activity context is required. A reference to this context is *not* retained by
     * the new instance.
     */
    init {
        // Resolve Material3 theme colors instead of AppCompat
        this.allTimesColor = fetchAttrColor(context, android.R.attr.colorPrimary)
        this.bestTimesColor = fetchAttrColor(context, R.attr.colorTertiary)
        this.limitLineColor = fetchAttrColor(context, R.attr.colorOutline)

        mExtraColors[0] = fetchAttrColor(context, R.attr.colorOnSurfaceVariant)
        mExtraColors[1] = fetchAttrColor(context, R.attr.colorOnTertiaryContainer)
        mExtraColors[2] = fetchAttrColor(context, R.attr.colorOnSecondaryContainer)

        this.allTimesLabel = context.getString(R.string.graph_legend_all_times)
        this.bestTimesLabel = context.getString(R.string.graph_legend_best_times)
        this.averageOfNLabelPrefix = context.getString(R.string.graph_legend_avg_prefix)
        this.limitLineLabel = context.getString(R.string.graph_mean)
        this.dateFormatSpec = context.getString(R.string.shortDateFormat)
    }

    /**
     * Gets a color to use when representing a data set for extra information. For example, a data
     * set for the average-of-N times.
     * 
     * @param index The (zero-based) index of the extra color to be retrieved.
     * @return The extra color value.
     * 
     * @throws IllegalArgumentException
     * If the index is outside of the range for the number of extra colors supported by the
     * application themes.
     */
    fun getExtraColor(index: Int): Int {
        require(!(index < 0 || index >= MAX_EXTRA_COLORS)) { "Index '" + index + "' out of range. Only " + MAX_EXTRA_COLORS + " supported." }

        return mExtraColors[index]
    }

    companion object {
        // NOTE: Separating this from "ChartStatistics" avoid the need for that class to be given an
        // activity context (for theme attribute values), which allows it to be used from Loaders or
        // other tasks run on background threads.
        /**
         * The maximum number of "extra" colors supported. These correspond to the theme attributes
         * with the naming pattern `colorChartExtra*`. If more colors are required, more
         * attributes will need to be declared and then defined for each theme.
         */
        private const val MAX_EXTRA_COLORS = 3
    }
}
