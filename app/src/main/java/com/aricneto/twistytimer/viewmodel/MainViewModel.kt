package com.aricneto.twistytimer.viewmodel

import androidx.lifecycle.ViewModel
import com.aricneto.twistytimer.utils.PuzzleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    data class ExportParams(
        val puzzleType: String = PuzzleUtils.TYPE_333,
        val puzzleCategory: String = "Normal",
        val mode: Int = 0
    )

    private val _exportParams = MutableStateFlow(ExportParams())
    val exportParams: StateFlow<ExportParams> = _exportParams.asStateFlow()

    fun updateExportParams(puzzleType: String?, puzzleCategory: String?, mode: Int) {
        _exportParams.value = ExportParams(
            puzzleType = puzzleType ?: PuzzleUtils.TYPE_333,
            puzzleCategory = puzzleCategory ?: "Normal",
            mode = mode
        )
    }
}
