package com.aricneto.twistytimer.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.stats.ChartStatistics
import com.aricneto.twistytimer.stats.ChartStyle
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StatsGraphUiState(
    val allTimes: List<Pair<Float, Float>> = emptyList(),
    val bestTimes: List<Pair<Float, Float>> = emptyList(),
    val averages: Map<Int, List<Pair<Float, Float>>> = emptyMap(),
    val statistics: Statistics? = null,
    val improvementStats: List<String> = emptyList(),
    val averageStats: List<String> = emptyList(),
    val otherStats: List<String> = emptyList()
)

class TimerGraphViewModel : ViewModel() {

    data class GraphParams(
        val type: String? = null,
        val subtype: String? = null,
        val isForCurrentSessionOnly: Boolean = true,
        val mode: Int = 0
    )

    private val repository = TwistyTimer.getSolveRepository()

    private val _params = MutableStateFlow(GraphParams())
    val params: StateFlow<GraphParams> = _params.asStateFlow()

    private val _chartStatistics = MutableStateFlow<ChartStatistics?>(null)
    val chartStatistics: StateFlow<ChartStatistics?> = _chartStatistics.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsGraphUiState> = _chartStatistics
        .map { stats ->
            if (stats == null) return@map StatsGraphUiState()
            
            val ns = stats.statistics.nsOfAverages
            val averagesMap = mutableMapOf<Int, List<Pair<Float, Float>>>()
            ns.forEach { n ->
                 averagesMap[n] = stats.getAveragePoints(n)
            }

            StatsGraphUiState(
                allTimes = stats.getAllSolvePoints(),
                bestTimes = stats.getBestSolvePoints(),
                averages = averagesMap,
                statistics = stats.statistics
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsGraphUiState())

    private var chartStyle: ChartStyle? = null

    init {
        viewModelScope.launch {
            TTEventBus.events
                .filter { it.hasCategory(TTIntent.CATEGORY_TIME_DATA_CHANGES) }
                .collect { intent ->
                    when (intent.action) {
                        TTIntent.ACTION_TIME_ADDED -> {
                            if (!deliverQuickResult(intent)) {
                                refreshChartStatistics()
                            }
                        }
                        TTIntent.ACTION_TIMES_MOVED_TO_HISTORY -> {
                            if (_params.value.isForCurrentSessionOnly) {
                                refreshChartStatistics()
                            }
                        }
                        TTIntent.ACTION_TIMES_MODIFIED -> {
                            refreshChartStatistics()
                        }
                        TTIntent.ACTION_SESSION_TIMES_SHOWN -> {
                            updateSessionOnly(true)
                        }
                        TTIntent.ACTION_HISTORY_TIMES_SHOWN -> {
                            updateSessionOnly(false)
                        }
                    }
                }
        }
    }

    fun setChartStyle(style: ChartStyle) {
        this.chartStyle = style
        // Trigger initial load if params are already set
        if (_chartStatistics.value == null) {
            refreshChartStatistics()
        }
    }

    fun updateParams(type: String?, subtype: String?, isForCurrentSessionOnly: Boolean, mode: Int) {
        val oldParams = _params.value
        _params.value = GraphParams(type, subtype, isForCurrentSessionOnly, mode)
        
        if (type != oldParams.type || subtype != oldParams.subtype || mode != oldParams.mode || isForCurrentSessionOnly != oldParams.isForCurrentSessionOnly) {
            refreshChartStatistics()
        }
    }

    private fun updateSessionOnly(isForCurrentSessionOnly: Boolean) {
        if (_params.value.isForCurrentSessionOnly != isForCurrentSessionOnly) {
            _params.value = _params.value.copy(isForCurrentSessionOnly = isForCurrentSessionOnly)
            refreshChartStatistics()
        }
    }

    fun refreshChartStatistics() {
        val style = chartStyle ?: return
        val currentParams = _params.value
        if (currentParams.type != null && currentParams.subtype != null) {
            viewModelScope.launch {
                val stats = if (currentParams.isForCurrentSessionOnly)
                    ChartStatistics.newCurrentSessionChartStatistics(style)
                else
                    ChartStatistics.newAllTimeChartStatistics(style)

                repository.populateChartStatistics(
                    currentParams.type,
                    currentParams.subtype,
                    currentParams.mode,
                    stats
                )
                _chartStatistics.value = stats
            }
        }
    }

    private fun deliverQuickResult(intent: Intent): Boolean {
        val stats = _chartStatistics.value ?: return false
        val solve = TTIntent.getSolve(intent) ?: return false
        val currentParams = _params.value

        if (solve.mode == currentParams.mode) {
            if (solve.penalty == PuzzleUtils.PENALTY_DNF) {
                stats.addDNF(solve.date)
            } else {
                stats.addTime(solve.time.toLong(), solve.date)
            }
            // Trigger flow update by re-assigning the same object (might need a new wrapper or different approach if UI doesn't update)
            // But StateFlow only emits if the value changes. ChartStatistics is mutable here.
            // Re-wrapping or using a simple version counter could work.
            _chartStatistics.value = null 
            _chartStatistics.value = stats
            return true
        }
        return false
    }
}
