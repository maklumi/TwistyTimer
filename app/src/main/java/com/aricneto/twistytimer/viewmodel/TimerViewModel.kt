package com.aricneto.twistytimer.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    data class SolveParams(
        val type: String? = null,
        val subtype: String? = null,
        val mode: Int = 0,
        val history: Boolean = false,
        val search: String = "",
        val orderByKey: String = SolveRepository.KEY_DATE,
        val orderByDir: String = SolveRepository.DIR_DESC
    )

    private val repository = TwistyTimer.getSolveRepository()

    private val _params = MutableStateFlow(SolveParams())
    val params: StateFlow<SolveParams> = _params.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val solves: StateFlow<List<Solve>> = _params
        .flatMapLatest { params ->
            if (params.type != null && params.subtype != null) {
                repository.getSolves(
                    params.type,
                    params.subtype,
                    params.mode,
                    params.history,
                    params.search,
                    params.orderByKey,
                    params.orderByDir
                )
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    init {
        viewModelScope.launch {
            TTEventBus.events
                .filter { it.hasCategory(TTIntent.CATEGORY_TIME_DATA_CHANGES) }
                .collect { intent ->
                    when (intent.action) {
                        TTIntent.ACTION_TIME_ADDED -> {
                            if (!deliverQuickResult(intent)) {
                                refreshStatistics()
                            }
                        }
                        TTIntent.ACTION_TIMES_MODIFIED, TTIntent.ACTION_TIMES_MOVED_TO_HISTORY -> {
                            refreshStatistics()
                        }
                    }
                }
        }
    }

    fun updateParams(
        type: String?,
        subtype: String?,
        mode: Int,
        history: Boolean,
        search: String = "",
        key: String = SolveRepository.KEY_DATE,
        dir: String = SolveRepository.DIR_DESC
    ) {
        val oldParams = _params.value
        _params.value = SolveParams(type, subtype, mode, history, search, key, dir)
        
        // Refresh statistics if puzzle or mode changed
        if (type != oldParams.type || subtype != oldParams.subtype || mode != oldParams.mode) {
            refreshStatistics()
        }
    }

    fun refreshStatistics() {
        val currentParams = _params.value
        if (currentParams.type != null && currentParams.subtype != null) {
            viewModelScope.launch {
                val stats = Statistics.newAllTimeStatistics()
                repository.populateStatistics(
                    currentParams.type,
                    currentParams.subtype,
                    currentParams.mode,
                    stats
                )
                _statistics.value = stats
            }
        }
    }

    private fun deliverQuickResult(intent: Intent): Boolean {
        val stats = _statistics.value ?: return false
        val solve = TTIntent.getSolve(intent) ?: return false
        val currentParams = _params.value

        if (solve.mode == currentParams.mode) {
            if (solve.penalty == PuzzleUtils.PENALTY_DNF) {
                stats.addDNF(true)
            } else {
                stats.addTime(solve.time.toLong(), true)
            }
            _statistics.value = stats
            return true
        }
        return false
    }

    fun deleteSolves(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteSolvesByID(ids, null)
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
        }
    }
}
