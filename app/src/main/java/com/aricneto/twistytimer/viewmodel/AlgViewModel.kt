package com.aricneto.twistytimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.AlgRepository
import com.aricneto.twistytimer.items.Algorithm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class AlgViewModel : ViewModel() {

    private val repository = AlgRepository(TwistyTimer.getDBHandler())

    private val _subset = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val algorithms: StateFlow<List<Algorithm>> = _subset
        .flatMapLatest { subset ->
            if (subset != null) {
                repository.getAlgorithms(subset)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSubset(subset: String?) {
        _subset.value = subset
    }
}
