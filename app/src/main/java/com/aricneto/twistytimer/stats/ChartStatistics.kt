package com.aricneto.twistytimer.stats

/**
 * A collector for solve times and related statistics (average times) to be presented in a chart.
 */
class ChartStatistics private constructor(
    val statistics: Statistics,
    val isForCurrentSessionOnly: Boolean
) {
    private val mNsOfAverages: IntArray = statistics.nsOfAverages
    private var mXIndex = 0
    private var mBestTime: Long = Long.MAX_VALUE

    private val allSolvePoints = mutableListOf<Pair<Float, Float>>()
    private val bestSolvePoints = mutableListOf<Pair<Float, Float>>()
    private val averagePointsMap = mutableMapOf<Int, MutableList<Pair<Float, Float>>>()

    init {
        require(statistics.isForCurrentSessionOnly) { "Statistics must be for current session only." }
        reset()
    }

    /**
     * Resets all chart data and statistics to their initial, empty state.
     */
    fun reset() {
        statistics.reset()
        mXIndex = 0
        mBestTime = Long.MAX_VALUE
        allSolvePoints.clear()
        bestSolvePoints.clear()
        averagePointsMap.clear()
        for (n in mNsOfAverages) {
            averagePointsMap[n] = mutableListOf()
        }
    }

    /**
     * Adds a solve time to the statistics and chart dataset.
     */
    fun addTime(time: Long, date: Long) {
        statistics.addTime(time, true)

        var isEntryAdded = false

        if (time != AverageCalculator.DNF) {
            allSolvePoints.add(mXIndex.toFloat() to (time / 1000f))
            isEntryAdded = true

            if (time < mBestTime) {
                mBestTime = time
                bestSolvePoints.add(mXIndex.toFloat() to (mBestTime / 1000f))
            }
        }

        for (n in mNsOfAverages) {
            val ac = statistics.getAverageOf(n, true)
            val averageTime = ac?.currentAverage ?: AverageCalculator.UNKNOWN

            if (averageTime != AverageCalculator.DNF && averageTime != AverageCalculator.UNKNOWN) {
                val aonYValue = averageTime / 1000f
                averagePointsMap[n]?.add(mXIndex.toFloat() to aonYValue)
                isEntryAdded = true
            }
        }

        if (isEntryAdded) {
            mXIndex++
        }
    }

    /**
     * Records a did-not-finish (DNF) solve.
     */
    fun addDNF(date: Long) {
        addTime(AverageCalculator.DNF, date)
    }

    fun getAllSolvePoints(): List<Pair<Float, Float>> = allSolvePoints.toList()

    fun getBestSolvePoints(): List<Pair<Float, Float>> = bestSolvePoints.toList()

    fun getAveragePoints(n: Int): List<Pair<Float, Float>> = averagePointsMap[n]?.toList() ?: emptyList()

    companion object {
        fun newAllTimeChartStatistics(chartStyle: ChartStyle): ChartStatistics {
            return ChartStatistics(
                Statistics.newAllTimeAveragesChartStatistics(),
                false
            )
        }

        fun newCurrentSessionChartStatistics(chartStyle: ChartStyle): ChartStatistics {
            return ChartStatistics(
                Statistics.newCurrentSessionAveragesChartStatistics(),
                true
            )
        }
    }
}
