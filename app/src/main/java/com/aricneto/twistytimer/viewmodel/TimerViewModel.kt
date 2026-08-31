package com.aricneto.twistytimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun updateParams(
        type: String?,
        subtype: String?,
        mode: Int,
        history: Boolean,
        search: String = "",
        key: String = SolveRepository.KEY_DATE,
        dir: String = SolveRepository.DIR_DESC
    ) {
        _params.value = SolveParams(type, subtype, mode, history, search, key, dir)
    }

    fun deleteSolves(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteSolvesByID(ids, null)
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
        }
    }
}
