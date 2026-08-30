package com.aricneto.twistytimer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.FragmentTimerGraphBinding
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.adapter.StatGridAdapter
import com.aricneto.twistytimer.fragment.TimerGraphFragment.Companion.newInstance
import com.aricneto.twistytimer.items.Stat
import com.aricneto.twistytimer.spans.RoundedAxisValueFormatter
import com.aricneto.twistytimer.spans.TimeFormatter
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.tr
import com.aricneto.twistytimer.stats.ChartStatistics
import com.aricneto.twistytimer.stats.ChartStatisticsLoader
import com.aricneto.twistytimer.stats.ChartStyle
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.stats.StatisticsCache
import com.aricneto.twistytimer.stats.StatisticsCache.StatisticsObserver
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.utils.Wrapper
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import java.util.Locale

/* Things that must be hidden/shown when refreshing the card.
    @BindViews({
            R.id.personalBestTitle, R.id.sessionBestTitle, R.id.sessionCurrentTitle,
            R.id.horizontalDivider02, R.id.verticalDivider02, R.id.verticalDivider03,
    }) View[] statisticsTableViews;*/

/**
 * A simple [Fragment] subclass.
 * Use the [newInstance] factory method to
 * create an instance of this fragment.
 */
class TimerGraphFragment : Fragment(), StatisticsObserver {
    private var currentPuzzle: String? = null
    private var currentPuzzleSubtype: String? = null
    private var history = false

    private var binding: FragmentTimerGraphBinding? = null

    private var statsImprovementGridView: GridView? = null
    private var statsAverageGridView: GridView? = null
    private var statsOtherGridView: GridView? = null
    private var statsImprovementLabelGridView: GridView? = null
    private var statsAverageLabelGridView: GridView? = null
    private var statsOtherLabelGridView: GridView? = null

    private var buttonDrawable: Drawable? = null
    private var buttonDrawableFaded: Drawable? = null


    /*
    @OnClick( {R.id.stats_label, R.id.stats_global, R.id.stats_session, R.id.stats_current} )
    public void onClickStats(View view) {
        String label = "";
        switch (view.getId()) {
            case R.id.stats_label:
                label = getString(R.string.graph_stats_average_label);
                break;
            case R.id.stats_global:
                label = getString(R.string.graph_stats_average_global);
                break;
            case R.id.stats_session:
                label = getString(R.string.graph_stats_average_session);
                break;
            case R.id.stats_current:
                label = getString(R.string.graph_stats_average_current);
                break;

        }
        Toast.makeText(mContext, label, Toast.LENGTH_LONG).show();
    }*/
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=$savedInstanceState)")
        super.onCreate(savedInstanceState)

        mContext = context

        if (arguments != null) {
            currentPuzzle = requireArguments().getString(PUZZLE)
            currentPuzzleSubtype = requireArguments().getString(PUZZLE_SUBTYPE)
            history = requireArguments().getBoolean(HISTORY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=$savedInstanceState)")
        binding = FragmentTimerGraphBinding.inflate(inflater, container, false)

        return binding!!.getRoot()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Drawables for the stats cards buttons
        buttonDrawable = ThemeUtils.createSquareDrawable(
            mContext!!,
            ThemeUtils.fetchAttrColor(mContext!!, R.attr.graph_stats_card_background),
            0, 20, 0f
        )
        buttonDrawableFaded = ThemeUtils.createSquareDrawable(
            mContext!!,
            ThemeUtils.fetchAttrColor(mContext!!, R.attr.graph_stats_card_background_faded),
            0, 20, 0f
        )

        // Setting for landscape mode. The chart and statistics table need to be scrolled, as the
        // statistics table will likely almost fill the screen. The automatic layout causes the
        // chart to use only the remaining space after the statistics table takes its space.
        // However, this may lead to the whole chart being squeezed into a few vertical pixels.
        // Therefore, set a fixed height for the chart that will force the statistics table to be
        // scrolled down to allow the chart to fit.
        binding!!.statsCard.post {
            if (binding?.linechart != null) {
                val chartParams = binding!!.linechart.layoutParams
                val cardHeight = binding!!.statsCard.height
                // ATTENTION: 134dp is the sum of the actionBarPadding and tabBarPadding attributes,
                // plus 16 dp for the view padding! Keep these in sync.
                val viewHeight = (view.height - ThemeUtils.dpToPix(mContext!!, 134f))
                if (chartParams != null) {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        chartParams.height = viewHeight
                        binding!!.linechart.layoutParams = chartParams
                        binding!!.linechart.requestLayout()
                    } else {
                        // On portrait mode, if the stats card occupies less than 40% of the view,
                        // the graph should fill all remaining space.
                        // If the stats card is bigger than 40%, the graph should occupy 70% of the view,
                        // and the user will have to scroll down to see the card
                        Log.d(
                            TAG,
                            "card: " + cardHeight + " | view: " + viewHeight + " | div: " + (cardHeight.toFloat() / viewHeight.toFloat())
                        )
                        if ((cardHeight.toFloat() / viewHeight.toFloat()) <= 0.4f) chartParams.height =
                            viewHeight - cardHeight
                        else chartParams.height = (viewHeight * 0.7f).toInt()

                        binding!!.linechart.layoutParams = chartParams
                        binding!!.linechart.requestLayout()
                    }
                }
            }
        }

        // The color for the text in the legend and for the values along the chart's axes.
        val chartTextColor = ThemeUtils.fetchAttrColor(mContext!!, R.attr.colorChartText)

        // The color for the grid and the axes
        val axisColor = ThemeUtils.fetchAttrColor(mContext!!, R.attr.colorChartAxis)
        val gridColor = ThemeUtils.fetchAttrColor(mContext!!, R.attr.colorChartGrid)

        // Most of the following settings should be self-explanatory.
        // Those that aren't will be commented

        // General chart settings
        //lineChartView.setPinchZoom(true);
        binding!!.linechart.setBackgroundColor(Color.TRANSPARENT)
        binding!!.linechart.setDrawGridBackground(false)
        binding!!.linechart.axisLeft.isEnabled = false
        binding!!.linechart.legend.textColor = chartTextColor
        binding!!.linechart.extraBottomOffset = ThemeUtils.dpToPix(mContext!!, 4f).toFloat()
        binding!!.linechart.description = null

        // Set axis colors
        val axisLeft = binding!!.linechart.axisRight
        val xAxis = binding!!.linechart.xAxis

        // X-axis settings
        xAxis.setDrawGridLines(false)
        // Draw X line markings on the bottom
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisLineColor = axisColor
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = chartTextColor
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.valueFormatter = RoundedAxisValueFormatter(xAxis.mDecimals)

        axisLeft.setDrawGridLines(true)
        //axisLeft.setSpaceTop(30f);
        axisLeft.textColor = axisColor
        axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        axisLeft.axisLineColor = axisColor
        axisLeft.setDrawAxisLine(false)
        axisLeft.gridColor = gridColor
        axisLeft.enableGridDashedLine(10f, 8f, 0f)
        axisLeft.valueFormatter = TimeFormatter()
        axisLeft.setDrawLimitLinesBehindData(true)


        // Find the gridView inside each included layout
        statsImprovementGridView = binding!!.statsContainerPager.statsTableImprovement.statsGridView
        statsAverageGridView = binding!!.statsContainerPager.statsTableAverage.statsGridView
        statsOtherGridView = binding!!.statsContainerPager.statsTableOther.statsGridView

        // And the label gridview...
        // We need a separate gridview for the label because gridView doesn't support staggered
        // layouts, and we need the label gridview to be slightly smaller for aesthetic reasons
        statsImprovementLabelGridView =
            binding!!.statsContainerPager.statsTableImprovement.statsLabelGridView
        statsAverageLabelGridView =
            binding!!.statsContainerPager.statsTableAverage.statsLabelGridView
        statsOtherLabelGridView = binding!!.statsContainerPager.statsTableOther.statsLabelGridView

        // The "Improvement" and "Other" grids should only have two columns. (Best and Session)
        // Since the base layout is 3-columns wide, we have to hide the last column title and set
        // num of columns to 2 for these two views
        binding!!.statsContainerPager.statsTableImprovement.statsCurrent.visibility = View.GONE
        binding!!.statsContainerPager.statsTableOther.statsCurrent.visibility = View.GONE
        statsImprovementGridView!!.numColumns = 2
        statsOtherGridView!!.numColumns = 2

        // Set stats name tooltips (label shown on long press)
        setTooltipText(R.id.stats_global, R.string.graph_stats_title_best_all_time)
        setTooltipText(R.id.stats_session, R.string.graph_stats_title_session_best)
        setTooltipText(R.id.stats_current, R.string.graph_stats_title_current)

        // Finally, name the label column.
        statsImprovementLabelGridView!!.adapter = StatGridAdapter(
            mContext!!,
            buildLabelList(R.array.stats_column_improvement)
        )
        statsAverageLabelGridView!!.adapter = StatGridAdapter(
            mContext!!,
            buildLabelList(R.array.stats_column_average)
        )
        statsOtherLabelGridView!!.adapter = StatGridAdapter(
            mContext!!,
            buildLabelList(R.array.stats_column_other)
        )


        binding!!.statsContainerPager.statsTableViewflipper.setInAnimation(
            mContext,
            R.anim.stats_grid_in
        )
        binding!!.statsContainerPager.statsTableViewflipper.setOutAnimation(
            mContext,
            R.anim.stats_grid_out
        )
        binding!!.statsContainerPager.statsTabImprovement.setOnClickListener(statTabClickListener)
        binding!!.statsContainerPager.statsTabAverage.setOnClickListener(statTabClickListener)
        binding!!.statsContainerPager.statsTabOther.setOnClickListener(statTabClickListener)

        highlightStatTab(binding!!.statsContainerPager.statsTabImprovement)
        fadeStatTab(binding!!.statsContainerPager.statsTabOther)
        fadeStatTab(binding!!.statsContainerPager.statsTabAverage)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now. If the statistics are non-null, they will be displayed.
        // If they are null (i.e., not yet loaded), the progress bar will be displayed until this
        // fragment, as a registered observer, is notified when loading is complete. Post the
        // firing of the event, so that it is received after "onCreateView" returns.
        onStatisticsUpdated(StatisticsCache.instance.statistics)
        StatisticsCache.instance.registerObserver(this) // Unregistered in "onDestroyView".

        // "restartLoader" ensures that any old loader with the wrong puzzle type/subtype will not
        // be reused. For now, those arguments are just passed via their respective fields to
        // "onCreateLoader".
        //
        // Starting loaders here in "onActivityCreated" ensures that "onCreateView" is complete.
        //
        // An anonymous inner class is neater than implementing "LoaderCallbacks".
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated -> restartLoader: CHART_DATA_LOADER_ID")
        getLoaderManager().restartLoader<Wrapper<ChartStatistics?>?>(
            MainActivity.CHART_DATA_LOADER_ID, null,
            object : LoaderManager.LoaderCallbacks<Wrapper<ChartStatistics?>?> {
                override fun onCreateLoader(
                    id: Int,
                    args: Bundle?
                ): Loader<Wrapper<ChartStatistics?>?> {
                    if (DEBUG_ME) Log.d(TAG, "onCreateLoader: CHART_DATA_LOADER_ID")
                    // "ChartStyle" allows the Loader to be executed without the need to hold a
                    // reference to an Activity context (required to access theme attributes),
                    // which would be likely to cause memory leaks and crashes.
                    return ChartStatisticsLoader(
                        requireContext(), ChartStyle(requireActivity()), currentPuzzle,
                        currentPuzzleSubtype, !history
                    )
                }

                override fun onLoadFinished(
                    loader: Loader<Wrapper<ChartStatistics?>?>,
                    data: Wrapper<ChartStatistics?>?
                ) {
                    if (DEBUG_ME) Log.d(TAG, "onLoadFinished: CHART_DATA_LOADER_ID")
                    updateChart(data?.content()!!)
                }

                override fun onLoaderReset(loader: Loader<Wrapper<ChartStatistics?>?>) {
                    if (DEBUG_ME) Log.d(TAG, "onLoaderReset: CHART_DATA_LOADER_ID")
                    // Nothing to do here, as the "ChartStatistics" object was never retained.
                    // The view is most likely destroyed at this time, so no need to update it.
                }
            })
    }

    override fun onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
        binding = null
        StatisticsCache.instance.unregisterObserver(this)
    }

    /**
     * Since all stats views have the same base layout, they share the same views
     * with the same ids. This function uses the TooltipCompat function added in support library
     * v26 to set a tooltip for an icon in each of the stats tabs.
     * 
     * @param viewId
     * The ID of the view that should receive the tooltip
     * @param tooltipTextRes
     * The string resource to be displayed
     */
    private fun setTooltipText(@IdRes viewId: Int, @StringRes tooltipTextRes: Int) {
        TooltipCompat.setTooltipText(
            binding!!.statsContainerPager.statsTableImprovement.getRoot()
                .findViewById(viewId),
            getString(tooltipTextRes)
        )
        TooltipCompat.setTooltipText(
            binding!!.statsContainerPager.statsTableAverage.getRoot().findViewById(viewId),
            getString(tooltipTextRes)
        )
        TooltipCompat.setTooltipText(
            binding!!.statsContainerPager.statsTableOther.getRoot().findViewById(viewId),
            getString(tooltipTextRes)
        )
    }

    private fun buildLabelList(@ArrayRes stringArrayRes: Int): ArrayList<Stat> {
        val statList = ArrayList<Stat>()
        // Used to alternate background colors in foreach

        for ((row, label) in resources.getStringArray(stringArrayRes).withIndex()) {
            statList.add(Stat(label, row))
        }
        return statList
    }

    private fun highlightStatTab(tab: TextView) {
        tab.setTextColor(ThemeUtils.fetchAttrColor(mContext!!, R.attr.graph_stats_card_text_color))
        tab.background = buttonDrawable
    }

    private fun fadeStatTab(tab: TextView) {
        tab.setTextColor(
            ThemeUtils.fetchAttrColor(
                mContext!!, R.attr
                    .graph_stats_card_text_color_faded
            )
        )
        tab.background = buttonDrawableFaded
    }

    private val statTabClickListener: View.OnClickListener = View.OnClickListener { tab ->
        when (tab.id) {
            R.id.stats_tab_improvement -> if (binding!!.statsContainerPager.statsTableViewflipper.displayedChild != TAB_FAVORITE) {
                binding!!.statsContainerPager.statsTableViewflipper.displayedChild = TAB_FAVORITE
                highlightStatTab(binding!!.statsContainerPager.statsTabImprovement)
                fadeStatTab(binding!!.statsContainerPager.statsTabOther)
                fadeStatTab(binding!!.statsContainerPager.statsTabAverage)
            }

            R.id.stats_tab_average -> if (binding!!.statsContainerPager.statsTableViewflipper.displayedChild != TAB_AVERAGE) {
                binding!!.statsContainerPager.statsTableViewflipper.displayedChild = TAB_AVERAGE
                highlightStatTab(binding!!.statsContainerPager.statsTabAverage)
                fadeStatTab(binding!!.statsContainerPager.statsTabOther)
                fadeStatTab(binding!!.statsContainerPager.statsTabImprovement)
            }

            R.id.stats_tab_other -> if (binding!!.statsContainerPager.statsTableViewflipper.displayedChild != TAB_OTHER) {
                binding!!.statsContainerPager.statsTableViewflipper.displayedChild = TAB_OTHER
                highlightStatTab(binding!!.statsContainerPager.statsTabOther)
                fadeStatTab(binding!!.statsContainerPager.statsTabImprovement)
                fadeStatTab(binding!!.statsContainerPager.statsTabAverage)
            }
        }
    }


    /**
     * Sets the visibility of the statistics table values columns. When loading starts, the columns
     * should be hidden and a progress bar shown. When loading finishes, the columns should be
     * populated with values and be shown and the progress bar hidden. No action will be taken if
     * this fragment does not yet have a view, or if its view has been destroyed.
     * 
     * @param visibility
     * The visibility to set on the statistics table columns. Use `View.GONE` or
     * `View.VISIBLE`. The opposite visibility will be applied to the progress bar.
     */
    private fun setStatsTableVisibility(visibility: Int) {
        if (view == null) {
            // Called before "onCreateView" or after "onDestroyView", so do nothing.
            return
        }
    }

    /**
     * Called when the chart statistics loader has completed loading the chart data. The loader
     * listens for changes to the data and this method will be called each time the display of the
     * chart needs to be refreshed. If this fragment has no view, no update will be attempted.
     * 
     * @param chartStats The chart statistics populated by the loader.
     */
    private fun updateChart(chartStats: ChartStatistics) {
        if (DEBUG_ME) Log.d(TAG, "updateChart()")

        if (view == null) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return
        }

        // Add all times line, best times line, average-of-N times lines (with highlighted
        // best AoN times) and mean limit line and identify them all using a custom legend.
        chartStats.applyTo(binding!!.linechart)

        // Animate and refresh the chart.
        binding!!.linechart.animateY(700, Easing.EaseInOutSine)
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view, no update will be
     * attempted.
     * 
     * @param stats
     * The updated statistics. These will not be modified. If `null`, a progress bar will
     * be displayed until non-`null` statistics are passed to this method in a later call.
     */
    @SuppressLint("SetTextI18n")
    override fun onStatisticsUpdated(stats: Statistics?) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")")

        if (view == null) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return
        }

        if (stats == null) {
            // Hide the statistics and show the progress bar until the statistics become ready.
            setStatsTableVisibility(View.GONE)
            return
        }

        // "tr()" converts from "AverageCalculator.UNKNOWN" and "AverageCalculator.DNF" to the
        // values needed by "convertTimeToString".
        val averageList = buildAverageList(stats)
        val otherList = buildOtherStatList(stats)
        val improvementList = buildImprovementStatList(stats)
        statsAverageGridView!!.adapter = StatGridAdapter(mContext!!, averageList)
        statsOtherGridView!!.adapter = StatGridAdapter(mContext!!, otherList)
        statsImprovementGridView!!.adapter = StatGridAdapter(mContext!!, improvementList)


        // Display the statistics and hide the progress bar.
        setStatsTableVisibility(View.VISIBLE)
    }

    private fun buildImprovementStatList(stats: Statistics): ArrayList<Stat> {
        // There are 2 columns (best, session), and 6 rows
        val statsList = ArrayList<Stat>(5 * 2)


        // DO NOT CHANGE THE ORDER!
        // The adapter adds views in the list order, left -> right, top -> bottom, so if the order
        // is changed, times will be placed in the wrong spots on the grid!
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeStdDeviation), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 0
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionStdDeviation), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 0
            )
        )

        // Ao12
        // all-time best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(12, false)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 1
            )
        )
        // session best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(12, true)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 1
            )
        )

        // Ao50
        // all-time best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(50, false)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 2
            )
        )
        // session best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(50, true)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 2
            )
        )

        // Ao100
        // all-time best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(100, false)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 3
            )
        )
        // session best
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(
                        stats.getAverageOf(100, true)!!
                            .bestAverage
                    ), PuzzleUtils.FORMAT_DEFAULT
                ),
                Stat.SCOPE_GLOBAL, 3
            )
        )

        // Best time
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeBestTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 4
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionBestTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 4
            )
        )

        // Num solves
        statsList.add(
            Stat(
                String.format(Locale.getDefault(), "%,d", stats.allTimeNumSolves),
                Stat.SCOPE_GLOBAL, 5
            )
        )
        statsList.add(
            Stat(
                String.format(Locale.getDefault(), "%,d", stats.sessionNumSolves),
                Stat.SCOPE_SESSION, 5
            )
        )

        return statsList
    }

    private fun buildOtherStatList(stats: Statistics): ArrayList<Stat> {
        // There are 2 columns (best, session), and 6 rows (best, worst, deviation, total time, mean
        // and count).
        val statsList = ArrayList<Stat>(5 * 2)


        // DO NOT CHANGE THE ORDER!
        // The adapter adds views in the list order, left -> right, top -> bottom, so if the order
        // is changed, times will be placed in the wrong spots on the grid!
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeBestTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 0
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionBestTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 0
            )
        )

        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeWorstTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 1
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionWorstTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 1
            )
        )

        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeStdDeviation), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 2
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionStdDeviation), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 2
            )
        )

        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeMeanTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_GLOBAL, 3
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionMeanTime), PuzzleUtils
                        .FORMAT_DEFAULT
                ), Stat.SCOPE_SESSION, 3
            )
        )

        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.allTimeTotalTime), PuzzleUtils
                        .FORMAT_LARGE
                ), Stat.SCOPE_GLOBAL, 4
            )
        )
        statsList.add(
            Stat(
                convertTimeToString(
                    tr(stats.sessionTotalTime), PuzzleUtils
                        .FORMAT_LARGE
                ), Stat.SCOPE_SESSION, 4
            )
        )

        statsList.add(
            Stat(
                String.format(Locale.getDefault(), "%,d", stats.allTimeNumSolves),
                Stat.SCOPE_GLOBAL, 5
            )
        )
        statsList.add(
            Stat(
                String.format(Locale.getDefault(), "%,d", stats.sessionNumSolves),
                Stat.SCOPE_SESSION, 5
            )
        )

        return statsList
    }

    /**
     * Builds a list of averages for use int [StatGridAdapter]
     * The order of the times is alternated (best, session, current, best, session...), so
     * it can work in an Adapter without much tinkering
     * @param stats
     * The [Statistics] instance
     * @return
     * An [<] containing the averages for the adapter
     */
    private fun buildAverageList(stats: Statistics): ArrayList<Stat> {
        val averageNumbers: IntArray = intArrayOf(3, 5, 12, 50, 100, 1000)
        // There are 3 columns (best, session, current), and 6 rows (the averages)
        // So the capacity of the list needs to be 3*6
        val statsList = ArrayList<Stat>(3 * 6)
        for (row in 0..5) {
            // best all time
            statsList.add(
                Stat(
                    convertTimeToString(
                        tr(
                            stats.getAverageOf(averageNumbers[row], false)!!
                                .bestAverage
                        ), PuzzleUtils.FORMAT_DEFAULT
                    ),
                    Stat.SCOPE_GLOBAL, row
                )
            )
            // session best
            statsList.add(
                Stat(
                    convertTimeToString(
                        tr(
                            stats.getAverageOf(averageNumbers[row], true)!!
                                .bestAverage
                        ), PuzzleUtils.FORMAT_DEFAULT
                    ),
                    Stat.SCOPE_GLOBAL, row
                )
            )
            // current
            statsList.add(
                Stat(
                    convertTimeToString(
                        tr(
                            stats.getAverageOf(averageNumbers[row], true)!!
                                .currentAverage
                        ), PuzzleUtils.FORMAT_DEFAULT
                    ),
                    Stat.SCOPE_CURRENT, row
                )
            )
        }
        return statsList
    }

    companion object {
        /**
         * Flag to enable debug logging for this class.
         */
        private const val DEBUG_ME = true

        /**
         * A "tag" to identify this class in log messages.
         */
        private val TAG: String = TimerGraphFragment::class.java.simpleName

        private const val PUZZLE = "puzzle"
        private const val PUZZLE_SUBTYPE = "puzzle_type"
        private const val HISTORY = "history"

        private const val TAB_FAVORITE = 0
        private const val TAB_AVERAGE = 1
        private const val TAB_OTHER = 2

        // We have to put a boolean history here because it resets when we change puzzles.
        fun newInstance(
            puzzle: String?,
            puzzleType: String?,
            history: Boolean
        ): TimerGraphFragment {
            val fragment = TimerGraphFragment()
            val args = Bundle()
            args.putString(PUZZLE, puzzle)
            args.putBoolean(HISTORY, history)
            args.putString(PUZZLE_SUBTYPE, puzzleType)
            fragment.setArguments(args)
            if (DEBUG_ME) Log.d(TAG, "newInstance() -> $fragment")
            return fragment
        }
    }
}
