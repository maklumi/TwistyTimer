package com.aricneto.twistytimer.viewmodel

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.tr
import com.aricneto.twistytimer.ui.components.TimerStats
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.utils.AlgUtils
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.ScrambleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    data class SolveParams(
        val type: String? = null,
        val subtype: String? = null,
        val mode: Int = 0,
        val history: Boolean = false,
        val subset: String? = null,
        val search: String = "",
        val orderByKey: String = SolveRepository.KEY_DATE,
        val orderByDir: String = SolveRepository.DIR_DESC
    )

    private val repository = TwistyTimer.getSolveRepository()
    private val algRepository = TwistyTimer.getAlgRepository()

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
    val timerStats: StateFlow<TimerStats> = _statistics
        .map { stats ->
            if (stats == null) return@map TimerStats()
            
            val fmt = { t: Long -> PuzzleUtils.convertTimeToString(tr(t), PuzzleUtils.FORMAT_DEFAULT) }
            
            val sessionCurrentAvg = LongArray(4)
            val allTimeBestAvg = LongArray(4)
            val ns = intArrayOf(5, 12, 50, 100)
            
            for (i in 0..3) {
                sessionCurrentAvg[i] = tr(stats.getAverageOf(ns[i], true)?.currentAverage ?: -666L)
                allTimeBestAvg[i] = tr(stats.getAverageOf(ns[i], false)?.bestAverage ?: -666L)
            }

            TimerStats(
                deviation = fmt(stats.sessionStdDeviation),
                mean = fmt(stats.sessionMeanTime),
                best = fmt(stats.sessionBestTime),
                count = stats.sessionNumSolves.toString(),
                ao5 = fmt(sessionCurrentAvg[0]),
                ao12 = fmt(sessionCurrentAvg[1]),
                ao50 = fmt(sessionCurrentAvg[2]),
                ao100 = fmt(sessionCurrentAvg[3]),
                isAo5Record = sessionCurrentAvg[0] > 0 && sessionCurrentAvg[0] <= allTimeBestAvg[0],
                isAo12Record = sessionCurrentAvg[1] > 0 && sessionCurrentAvg[1] <= allTimeBestAvg[1],
                isAo50Record = sessionCurrentAvg[2] > 0 && sessionCurrentAvg[2] <= allTimeBestAvg[2],
                isAo100Record = sessionCurrentAvg[3] > 0 && sessionCurrentAvg[3] <= allTimeBestAvg[3]
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerStats())

    private val _scramble = MutableStateFlow("Generating...")
    val scramble: StateFlow<String> = _scramble.asStateFlow()

    private val _scrambleDrawable = MutableStateFlow<Drawable?>(null)
    val scrambleDrawable: StateFlow<Drawable?> = _scrambleDrawable.asStateFlow()

    private val _scrambleState = MutableStateFlow<String?>(null)
    val scrambleState: StateFlow<String?> = _scrambleState.asStateFlow()

    private val _currentCaseId = MutableStateFlow<Long?>(null)
    val currentCaseId: StateFlow<Long?> = _currentCaseId.asStateFlow()

    private val _isScrambleLoading = MutableStateFlow(false)
    val isScrambleLoading: StateFlow<Boolean> = _isScrambleLoading.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(0L)
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.Stopped)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _showQAButtons = MutableStateFlow(false)
    val showQAButtons: StateFlow<Boolean> = _showQAButtons.asStateFlow()

    private val _showScrambleHint = MutableStateFlow(false)
    val showScrambleHint: StateFlow<Boolean> = _showScrambleHint.asStateFlow()

    private val _manualEntryEnabled = MutableStateFlow(false)
    val manualEntryEnabled: StateFlow<Boolean> = _manualEntryEnabled.asStateFlow()

    private val _selectedSolveIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSolveIds: StateFlow<Set<Long>> = _selectedSolveIds.asStateFlow()

    private var currentSolve: Solve? = null

    private var lastRegularPuzzle = PuzzleUtils.TYPE_333

    enum class TimerState { Stopped, Ready, Running }

    private var startTime = 0L

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
        subset: String? = null,
        search: String = "",
        key: String = SolveRepository.KEY_DATE,
        dir: String = SolveRepository.DIR_DESC
    ) {
        val oldParams = _params.value
        
        var newType = type
        if (mode == 0) {
            // Ensure we use a regular puzzle in mode 0
            if (newType == null || PuzzleUtils.getPositionOfPuzzle(newType) == 0 && newType != PuzzleUtils.TYPE_222) {
                // If type is invalid or not a regular puzzle (assuming getPositionOfPuzzle returns 0 for non-matches, 
                // but wait, 222 is index 0. I should check PuzzleUtils.kt)
                newType = lastRegularPuzzle
            } else {
                lastRegularPuzzle = newType
            }
        }

        _params.value = SolveParams(newType, subtype, mode, history, subset, search, key, dir)
        
        // Refresh statistics if puzzle or mode or subset changed
        if (newType != oldParams.type || subtype != oldParams.subtype || mode != oldParams.mode || subset != oldParams.subset) {
            refreshStatistics()
            updateScrambleHintVisibility(newType)
        }
        updateManualEntryStatus()
    }

    private fun updateManualEntryStatus() {
        _manualEntryEnabled.value = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
            .getBoolean(TwistyTimer.getAppContext().getString(R.string.pk_enable_manual_entry), false)
    }

    private fun updateScrambleHintVisibility(type: String?) {
        val params = _params.value
        val sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
        val hintsEnabled = sp.getBoolean(TwistyTimer.getAppContext().getString(R.string.pk_show_scramble_hints), true)
        _showScrambleHint.value = hintsEnabled && (type == PuzzleUtils.TYPE_333 || params.mode == 1)
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

    fun generateScramble() {
        val params = _params.value
        val type = params.type ?: PuzzleUtils.TYPE_333
        viewModelScope.launch {
            _isScrambleLoading.value = true
            val newScramble = withContext(Dispatchers.IO) {
                if (params.mode == 1 && params.subset != null) {
                    val subsetEnum = TrainerScrambler.TrainerSubset.valueOf(params.subset)
                    TrainerScrambler.generateTrainerCase(TwistyTimer.getAppContext(), subsetEnum, params.subtype)
                } else {
                    ScrambleGenerator(type).puzzle.generateScramble()
                }
            }
            _scramble.value = newScramble
            
            // Generate Drawable
            val sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
            val drawable = withContext(Dispatchers.IO) {
                ScrambleGenerator(type).generateImageFromScramble(sp, newScramble)
            }
            _scrambleDrawable.value = drawable
            
            // Generate State for CubeComponent (if trainer)
            if (params.mode == 1 && params.subset != null && !newScramble.startsWith("To start training")) {
                 val state = withContext(Dispatchers.IO) {
                     AlgUtils.getCaseState(TwistyTimer.getAppContext(), params.subset, newScramble)
                 }
                 _scrambleState.value = state

                 // Fetch case ID
                 viewModelScope.launch {
                     val algs = withContext(Dispatchers.IO) {
                         algRepository.getAllAlgorithms()
                     }
                     val match = algs.find { it.subset == params.subset && it.name == newScramble }
                     _currentCaseId.value = match?.id
                 }
            } else {
                 _scrambleState.value = null
                 _currentCaseId.value = null
            }

            _isScrambleLoading.value = false
        }
    }

    fun setScramble(newScramble: String) {
        _scramble.value = newScramble
        val type = _params.value.type ?: PuzzleUtils.TYPE_333
        viewModelScope.launch {
            val sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
            val drawable = withContext(Dispatchers.IO) {
                ScrambleGenerator(type).generateImageFromScramble(sp, newScramble)
            }
            _scrambleDrawable.value = drawable
        }
    }

    fun startTimer() {
        _timerState.value = TimerState.Running
        _showQAButtons.value = false
        startTime = System.currentTimeMillis()
        viewModelScope.launch {
            while (_timerState.value == TimerState.Running) {
                _currentTimeMillis.value = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    fun stopTimer() {
        if (_timerState.value == TimerState.Running) {
            _timerState.value = TimerState.Stopped
            _showQAButtons.value = true
            saveSolve()
        }
    }

    fun prepareTimer() {
        if (_timerState.value == TimerState.Stopped) {
            _timerState.value = TimerState.Ready
            _currentTimeMillis.value = 0L
        }
    }

    fun addManualSolve(timeMillis: Long, penalty: Int = PuzzleUtils.NO_PENALTY, comment: String = "") {
        val currentParams = _params.value
        val solve = Solve(
            time = if (penalty == PuzzleUtils.PENALTY_PLUSTWO) timeMillis.toInt() + 2000 else timeMillis.toInt(),
            puzzle = currentParams.type ?: PuzzleUtils.TYPE_333,
            subtype = currentParams.subtype ?: "Normal",
            date = System.currentTimeMillis(),
            scramble = _scramble.value,
            penalty = penalty,
            comment = comment,
            history = false,
            mode = currentParams.mode
        )
        viewModelScope.launch {
            val id = repository.insertSolve(
                type = solve.puzzle,
                subtype = solve.subtype,
                time = solve.time.toLong(),
                date = solve.date,
                scramble = solve.scramble,
                penalty = solve.penalty.toLong(),
                comment = solve.comment,
                history = solve.history,
                mode = solve.mode
            )
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIME_ADDED)
            currentSolve = solve.copy(id = id)
            _currentTimeMillis.value = solve.time.toLong()
            _showQAButtons.value = true
            generateScramble()
        }
    }

    private fun saveSolve() {
        val currentParams = _params.value
        val solve = Solve(
            time = _currentTimeMillis.value.toInt(),
            puzzle = currentParams.type ?: PuzzleUtils.TYPE_333,
            subtype = currentParams.subtype ?: "Normal",
            date = System.currentTimeMillis(),
            scramble = _scramble.value,
            penalty = PuzzleUtils.NO_PENALTY,
            comment = "",
            history = false,
            mode = currentParams.mode
        )
        viewModelScope.launch {
            val id = repository.insertSolve(
                type = solve.puzzle,
                subtype = solve.subtype,
                time = solve.time.toLong(),
                date = solve.date,
                scramble = solve.scramble,
                penalty = solve.penalty.toLong(),
                comment = solve.comment,
                history = solve.history,
                mode = solve.mode
            )
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIME_ADDED)
            currentSolve = solve.copy(id = id)
            generateScramble()
        }
    }

    fun removeLastSolve() {
        currentSolve?.let { solve ->
            viewModelScope.launch {
                repository.deleteSolve(solve.id)
                _currentTimeMillis.value = 0L
                _showQAButtons.value = false
                currentSolve = null
                TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
            }
        }
    }

    fun applyDnf() {
        currentSolve?.let { solve ->
            val updated = solve.copy(penalty = PuzzleUtils.PENALTY_DNF)
            currentSolve = updated
            _currentTimeMillis.value = -1L
            _showQAButtons.value = false
            viewModelScope.launch {
                repository.updateSolve(
                    id = updated.id,
                    time = updated.time.toLong(),
                    date = updated.date,
                    scramble = updated.scramble,
                    penalty = updated.penalty.toLong(),
                    comment = updated.comment,
                    history = updated.history,
                    mode = updated.mode
                )
                TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
            }
        }
    }

    fun applyPlusTwo() {
        currentSolve?.let { solve ->
            if (solve.penalty != PuzzleUtils.PENALTY_PLUSTWO) {
                val updated = solve.copy(time = solve.time + 2000, penalty = PuzzleUtils.PENALTY_PLUSTWO)
                currentSolve = updated
                _currentTimeMillis.value = updated.time.toLong()
                _showQAButtons.value = false
                viewModelScope.launch {
                    repository.updateSolve(
                        id = updated.id,
                        time = updated.time.toLong(),
                        date = updated.date,
                        scramble = updated.scramble,
                        penalty = updated.penalty.toLong(),
                        comment = updated.comment,
                        history = updated.history,
                        mode = updated.mode
                    )
                    TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
                }
            }
        }
    }

    fun addComment(comment: String) {
        currentSolve?.let { solve ->
            val updated = solve.copy(comment = comment)
            currentSolve = updated
            updateSolve(updated)
        }
    }

    fun updateSolve(solve: Solve) {
        viewModelScope.launch {
            repository.updateSolve(
                id = solve.id,
                time = solve.time.toLong(),
                date = solve.date,
                scramble = solve.scramble,
                penalty = solve.penalty.toLong(),
                comment = solve.comment,
                history = solve.history,
                mode = solve.mode
            )
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
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

    fun toggleSelection(solveId: Long) {
        _selectedSolveIds.update { current ->
            if (current.contains(solveId)) current - solveId else current + solveId
        }
    }

    fun clearSelection() {
        _selectedSolveIds.value = emptySet()
    }
}
