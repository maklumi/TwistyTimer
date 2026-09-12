package com.aricneto.twistytimer.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.FragmentTimerBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.fragment.dialog.AddTimeDialog
import com.aricneto.twistytimer.fragment.dialog.BottomSheetDetailDialog
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross
import com.aricneto.twistytimer.stats.AverageCalculator
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.tr
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.stats.StatisticsCache
import com.aricneto.twistytimer.stats.StatisticsCache.StatisticsObserver
import com.aricneto.twistytimer.utils.CountdownWarning
import com.aricneto.twistytimer.utils.DefaultPrefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.FORMAT_DEFAULT
import com.aricneto.twistytimer.utils.PuzzleUtils.NO_PENALTY
import com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_DNF
import com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_PLUSTWO
import com.aricneto.twistytimer.utils.PuzzleUtils.TYPE_333
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.aricneto.twistytimer.utils.ScrambleGenerator
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent.ACTION_COMMENT_ADDED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SCRAMBLE_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SCROLLED_PAGE
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STARTED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STOPPED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED_MANUALLY
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TOOLBAR_RESTORED
import com.aricneto.twistytimer.utils.TTIntent.BroadcastBuilder
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.TTIntent.getSolve
import com.aricneto.twistytimer.utils.ThemeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds


class TimerFragment : BaseFragment(), OnBackPressedInFragmentListener, StatisticsObserver {
    private var currentPuzzle: String? = null
    private var currentPuzzleCategory: String? = null
    private var currentSubset: TrainerSubset? = null

    /**
     * The last generated scramble, related to the current solve. When the timer is started,
     * the timer will generate a new scramble, but it will be saved in realScramble.
     */
    private var currentScramble: String? = ""

    /**
     * The scramble that is currently being shown to the user. MAY NOT BE currentScramble!
     */
    private var realScramble: String? = null

    private var currentSolve: Solve? = null

    var countdown: CountDownTimer? = null
    var countingDown: Boolean = false

    var firstWarning: CountdownWarning? = null
    var secondWarning: CountdownWarning? = null

    private var mContext: Context? = null

    // True If the show toolbar animation is done
    var animationDone: Boolean = true

    // True if the user has pressed the chronometer for long enough for it to start
    var isReady: Boolean = false

    // True If the user has holdEnabled and held the DNF at the last second
    var holdingDNF: Boolean = false

    // Checks if the chronometer is running. Has to be public so the main fragment can access it
    var isRunning: Boolean = false

    // Locks the chronometer so it doesn't start before a scramble sequence is generated
    var isLocked: Boolean = true

    // True If the chronometer has just been canceled
    private var isCanceled = false

    // True If the scrambler is done calculating and can calculate a new hint.
    private var canShowHint = false

    // Animation duration for all timer items
    private var mAnimationDuration = 0

    private var generator: ScrambleGenerator? = null

    private var scrambleJob: Job? = null
    private var optimalCrossJob: Job? = null

    private var currentPenalty: Int = NO_PENALTY

    /**
     * Specifies the current TimerMode
     */
    private var currentTimerMode: String? = null

    private var mCurrentAnimator: Animator? = null

    private var binding: FragmentTimerBinding? = null

    // Holds the localized strings related to each detail statistic, in order:
    // Ao5, Ao12, Ao50, Ao100, Deviation, Mean, Best, Count
    private var detailTextNamesArray: Array<String> = arrayOf("")

    private var buttonsEnabled = false
    private var scrambleImgEnabled = false
    private var sessionStatsEnabled = false
    private var worstSolveEnabled = false
    private var bestSolveEnabled = false
    private var scrambleEnabled = false
    private var scrambleBackgroundEnabled = false
    private var holdEnabled = false
    private var backCancelEnabled = false
    private var startCueEnabled = false
    private var showHintsEnabled = false
    private var showHintsXCrossEnabled = false
    private var averageRecordsEnabled = false

    /**
     * True if manual entry is enabled
     */
    private var manualEntryEnabled = false

    private var inspectionVibrationAlertEnabled = false
    private var inspectionSoundAlertEnabled = false

    var scrambleTextSize: Float = 0f

    // True if the user has started (and stopped) the timer at least once. Used to trigger
    // Average highlights, so the user doesn't get a notification when they start the app
    private var hasStoppedTimerOnce = false

    /**
     * The most recently notified solve time statistics. When [.addNewSolve] is called to
     * add a new time, the new time can be compared to these statistics to determine if the new
     * time sets a record.
     */
    private var mRecentStatistics: Statistics? = null

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                TTEventBus.events.collect { intent ->
                    if (intent.hasCategory(CATEGORY_UI_INTERACTIONS)) {
                        handleUIInteraction(intent)
                    }
                }
            }
        }
    }

    private fun handleUIInteraction(intent: Intent) {
        val binding = binding ?: return
        when (intent.action) {
            ACTION_SCROLLED_PAGE -> {
                if (holdEnabled) {
                    holdJob?.cancel()
                }
                binding.chronometer.setHighlighted(false)
                binding.chronometer.cancelHoldForStart()
                isReady = false
            }

            ACTION_TIME_ADDED_MANUALLY -> {
                currentSolve = getSolve(intent)
                val solve = currentSolve
                if (solve != null) {
                    binding.chronometer.text = HtmlCompat.fromHtml(
                        convertTimeToString(
                            solve.time.toLong(),
                            PuzzleUtils.FORMAT_SMALL_MILLI
                        ), HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    hideButtons(hideQuickActionButtons = true, hideUndoButton = true)
                    broadcastNewSolve()
                    declareRecordTimes(solve)
                }
            }

            ACTION_TOOLBAR_RESTORED -> {
                showItems()
                animationDone = true
                // Wait for animations to run before broadcasting solve to avoid UI stuttering
                viewLifecycleOwner.lifecycleScope.launch {
                    delay((mAnimationDuration + 50).toLong().milliseconds)
                    if (!isCanceled) {
                        // Only broadcast a new solve if it hasn't been canceled
                        broadcastNewSolve()
                    } else {
                        // The detail stats are triggered by a stats update.
                        // Since the solve has been canceled, there's no new stats
                        // to load, and it must be triggered manually
                        showDetailStats()
                    }
                    // reset isCanceled
                    isCanceled = false
                }
            }

            ACTION_GENERATE_SCRAMBLE -> generateNewScramble()
        }
    }

    private var holdJob: Job? = null
    private var plusTwoCountdown: CountDownTimer? = null

    private var optimalCross: RubiksCubeOptimalCross? = null
    private var optimalXCross: RubiksCubeOptimalXCross? = null
    private var scrambleDialog: BottomSheetDetailDialog? = null
    private var mFragManager: FragmentManager? = null

    private var currentModeInt: Int = 0

    private val buttonClickListener: View.OnClickListener = View.OnClickListener { view ->
        val solveRepository = TwistyTimer.getSolveRepository()

        // On most of these changes to the current solve, the Statistics and ChartStatistics
        // need to be updated to reflect the change. It would probably be too complicated to
        // add facilities to "AverageCalculator" to handle modification of the last added time
        // or an "undo" facility and then to integrate that into the loaders. Therefore, a full
        // reload will probably be required.
        when (view.id) {
            R.id.qa_remove -> MaterialAlertDialogBuilder(requireActivity())
                .setMessage(R.string.delete_dialog_confirmation_title)
                .setPositiveButton(
                    R.string.delete_dialog_confirmation_button
                ) { _: DialogInterface?, _: Int ->
                    currentSolve?.let { solve ->
                        lifecycleScope.launch {
                            solveRepository.deleteSolve(solve.id)
                            if (!isRunning) binding?.chronometer?.reset() // Reset to "0.00".

                            binding?.congratsText?.visibility = View.GONE
                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                        }
                    }
                    hideButtons(hideQuickActionButtons = true, hideUndoButton = true)
                }
                .setNegativeButton(R.string.delete_dialog_cancel_button, null)
                .show()

            R.id.qa_dnf -> {
                currentSolve?.let { solve ->
                    currentSolve = PuzzleUtils.applyPenalty(solve, PENALTY_DNF)
                    binding?.chronometer?.setPenalty(PENALTY_DNF)
                    lifecycleScope.launch {
                        solveRepository.updateSolve(
                            id = currentSolve!!.id,
                            time = currentSolve!!.time.toLong(),
                            date = currentSolve!!.date,
                            scramble = currentSolve!!.scramble,
                            penalty = currentSolve!!.penalty.toLong(),
                            comment = currentSolve!!.comment,
                            history = currentSolve!!.history,
                            mode = currentSolve!!.mode
                        )
                        hideButtons(hideQuickActionButtons = true, hideUndoButton = false)
                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                    }
                }
            }

            R.id.qa_plustwo -> {
                if (currentPenalty != PENALTY_PLUSTWO) {
                    currentSolve?.let { solve ->
                        currentSolve = PuzzleUtils.applyPenalty(solve, PENALTY_PLUSTWO)
                        binding?.chronometer?.setPenalty(PENALTY_PLUSTWO)
                        lifecycleScope.launch {
                            solveRepository.updateSolve(
                                id = currentSolve!!.id,
                                time = currentSolve!!.time.toLong(),
                                date = currentSolve!!.date,
                                scramble = currentSolve!!.scramble,
                                penalty = currentSolve!!.penalty.toLong(),
                                comment = currentSolve!!.comment,
                                history = currentSolve!!.history,
                                mode = currentSolve!!.mode
                            )
                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                        }
                    }
                }
                hideButtons(hideQuickActionButtons = true, hideUndoButton = false)
            }

            R.id.qa_comment -> {
                val commentView =
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_input, requireView().parent as ViewGroup, false)
                val commentEditText =
                    commentView.findViewById<TextInputEditText>(R.id.edit_text)
                commentEditText.setText(currentSolve?.comment)

                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.add_comment)
                    .setView(commentView)
                    .setPositiveButton(
                        R.string.action_done
                    ) { _: DialogInterface?, _: Int ->
                        currentSolve?.let { solve ->
                            solve.comment = commentEditText.text.toString()
                            lifecycleScope.launch {
                                solveRepository.updateSolve(
                                    id = solve.id,
                                    time = solve.time.toLong(),
                                    date = solve.date,
                                    scramble = solve.scramble,
                                    penalty = solve.penalty.toLong(),
                                    comment = solve.comment,
                                    history = solve.history,
                                    mode = solve.mode
                                )

                                broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_COMMENT_ADDED)
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.added_comment),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        hideButtons(hideQuickActionButtons = false, hideUndoButton = true)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.qa_undo -> {
                // Undo the setting of a DNF or +2 penalty (does not undo a delete or comment).
                currentSolve?.let { solve ->
                    currentSolve = PuzzleUtils.applyPenalty(solve, NO_PENALTY)
                    binding?.chronometer?.setPenalty(NO_PENALTY)
                    lifecycleScope.launch {
                        solveRepository.updateSolve(
                            id = currentSolve!!.id,
                            time = currentSolve!!.time.toLong(),
                            date = currentSolve!!.date,
                            scramble = currentSolve!!.scramble,
                            penalty = currentSolve!!.penalty.toLong(),
                            comment = currentSolve!!.comment,
                            history = currentSolve!!.history,
                            mode = currentSolve!!.mode
                        )
                        hideButtons(hideQuickActionButtons = false, hideUndoButton = true)
                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                    }
                }
            }

            R.id.scramble_button_reset -> broadcast(
                CATEGORY_UI_INTERACTIONS,
                ACTION_GENERATE_SCRAMBLE
            )

            R.id.scramble_button_edit -> {
                val editScrambleView =
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_input, requireView().parent as ViewGroup, false)
                val editScrambleEditText =
                    editScrambleView.findViewById<TextInputEditText>(R.id.edit_text)
                editScrambleEditText.setText(realScramble)

                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.edit_scramble)
                    .setView(editScrambleView)
                    .setPositiveButton(
                        R.string.action_done
                    ) { _: DialogInterface?, _: Int ->
                        setScramble(editScrambleEditText.text.toString())
                        // The hint solver will crash if you give it invalid scrambles,
                        // so we shouldn't calculate hints for custom scrambles.
                        // TODO: We can use the scramble image generator (which has a scramble validity checker) to check a scramble before calling a hint
                        canShowHint = false
                        hideButtons(hideQuickActionButtons = true, hideUndoButton = true)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.scramble_button_manual_entry -> {
                val addTimeDialog = AddTimeDialog.newInstance(
                    currentPuzzle,
                    currentPuzzleCategory,
                    realScramble,
                    currentModeInt
                )
                parentFragmentManager.let { addTimeDialog.show(it, "dialog_add_time") }
            }
        }
    }

    /**
     * Hides (or shows) the delete/dnf/plus-two quick action buttons and the undo button.
     */
    private fun hideButtons(hideQuickActionButtons: Boolean, hideUndoButton: Boolean) {
        binding?.qaButtons?.qaLayout?.visibility =
            if (hideQuickActionButtons) View.GONE else View.VISIBLE
        binding?.qaUndo?.visibility = if (hideUndoButton) View.GONE else View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=$savedInstanceState)")
        super.onCreate(savedInstanceState)
        mContext = context
        if (arguments != null) {
            currentPuzzle = requireArguments().getString(PUZZLE)
            currentPuzzleCategory = requireArguments().getString(PUZZLE_SUBTYPE)
            currentSubset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable(TRAINER_SUBSET, TrainerSubset::class.java)
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getSerializable(TRAINER_SUBSET) as TrainerSubset?
            }
            currentTimerMode = requireArguments().getString(TIMER_MODE)
            currentModeInt = modeToInt(currentTimerMode)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getString(PUZZLE) == requireArguments().getString(PUZZLE)) {
                realScramble = savedInstanceState.getString(SCRAMBLE)
            }
            currentTimerMode = savedInstanceState.getString(TIMER_MODE)
            currentModeInt = modeToInt(currentTimerMode)
            //hasStoppedTimerOnce = savedInstanceState.getBoolean(HAS_STOPPED_TIMER_ONCE, false);
        }

        detailTextNamesArray = resources.getStringArray(R.array.timer_detail_stats)

        this.newOptimalCross

        mFragManager = parentFragmentManager

        mAnimationDuration = getInt(
            R.string.pk_timer_animation_duration, requireContext().resources.getInteger(
                R.integer.defaultAnimationDuration
            )
        )

        generator = ScrambleGenerator(requireNotNull(currentPuzzle))
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=$savedInstanceState)")

        // Inflate the layout for this fragment
        binding = FragmentTimerBinding.inflate(inflater, container, false)

        return binding?.root
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
        val binding = binding ?: return

        // Necessary for the scramble image to show
        binding.scrambleImg.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        binding.expandedImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Set the zoom click listener
        binding.scrambleImg.setOnClickListener { _: View? ->
            zoomImageFromThumb(
                binding.scrambleImg
            )
        }


        // Retrieve and cache the system's default "short" animation time.
        binding.qaButtons?.let {
            it.qaRemove.setOnClickListener(buttonClickListener)
            it.qaDnf.setOnClickListener(buttonClickListener)
            it.qaPlustwo.setOnClickListener(buttonClickListener)
            it.qaComment.setOnClickListener(buttonClickListener)
        }
        binding.qaUndo.setOnClickListener(buttonClickListener)
        binding.scrambleBox.scrambleButtonReset.setOnClickListener(buttonClickListener)
        binding.scrambleBox.scrambleButtonEdit.setOnClickListener(buttonClickListener)

        // Preferences //
        val inspectionEnabled = getBoolean(R.string.pk_inspection_enabled, false)
        val inspectionTime = getInt(R.string.pk_inspection_time, 15)
        val timerTextSize = getInt(R.string.pk_timer_text_size, 100) / 100f
        val scrambleImageSize = getInt(R.string.pk_scramble_image_size, 100) / 100f
        scrambleTextSize = getInt(R.string.pk_scramble_text_size, 100) / 100f
        val advancedEnabled = getBoolean(R.string.pk_advanced_timer_settings_enabled, false)

        /*
         *  Scramble text size preference. It doesn't need to be in the "advanced" settings since
         *  it detects if it's clipping and automatically compensates for that by creating a button.
         */
        binding.scrambleBox.scrambleText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            binding.scrambleBox.scrambleText.textSize * scrambleTextSize
        )

        if (advancedEnabled) {
            binding.chronometer.setAutoSizeTextTypeUniformWithConfiguration(
                (90 * timerTextSize).toInt() / 2,
                (90 * timerTextSize).toInt(),
                2,
                TypedValue.COMPLEX_UNIT_SP
            )

            binding.scrambleImg.layoutParams.width =
                (binding.scrambleImg.layoutParams.width * scrambleImageSize).toInt()
            binding.scrambleImg.layoutParams.height =
                (binding.scrambleImg.layoutParams.height * calculateScrambleImageHeightMultiplier(
                    scrambleImageSize
                )).toInt()
        }

        val res = resources

        averageRecordsEnabled = getBoolean(
            R.string.pk_show_average_record_enabled,
            getBoolean(R.bool.default_showAverageRecordEnabled)
        )

        backCancelEnabled = getBoolean(
            R.string.pk_back_button_cancel_solve_enabled, res.getBoolean(
                R.bool.default_backCancelEnabled
            )
        )

        buttonsEnabled =
            getBoolean(R.string.pk_show_quick_actions, res.getBoolean(R.bool.default_buttonEnabled))
        holdEnabled = getBoolean(
            R.string.pk_hold_to_start_enabled,
            res.getBoolean(R.bool.default_holdEnabled)
        )
        startCueEnabled =
            getBoolean(R.string.pk_start_cue_enabled, res.getBoolean(R.bool.default_startCue))

        sessionStatsEnabled = getBoolean(R.string.pk_show_session_stats, true)
        bestSolveEnabled = getBoolean(R.string.pk_show_best_time, true)
        worstSolveEnabled = getBoolean(R.string.pk_show_worst_time, false)

        scrambleEnabled = getBoolean(R.string.pk_scramble_enabled, true)
        scrambleImgEnabled = getBoolean(R.string.pk_show_scramble_image, true)
        showHintsEnabled = getBoolean(R.string.pk_show_scramble_hints, true)
        showHintsXCrossEnabled = getBoolean(R.string.pk_show_scramble_x_cross_hints, false)

        manualEntryEnabled = getBoolean(R.string.pk_enable_manual_entry, false)

        scrambleBackgroundEnabled = getBoolean(R.string.pk_show_scramble_background, false)

        val inspectionAlertEnabled = getBoolean(R.string.pk_inspection_alert_enabled, false)
        val vibrationAlert = getString(R.string.pk_inspection_alert_vibration)
        val soundAlert = getString(R.string.pk_inspection_alert_sound)
        if (inspectionAlertEnabled) {
            val inspectionAlertType = Prefs.getString(
                R.string.pk_inspection_alert_type,
                getString(R.string.pk_inspection_alert_vibration)
            )
            when (inspectionAlertType) {
                vibrationAlert -> {
                    inspectionVibrationAlertEnabled = true
                    inspectionSoundAlertEnabled = false
                }

                soundAlert -> {
                    inspectionVibrationAlertEnabled = false
                    inspectionSoundAlertEnabled = true
                }

                else -> {
                    inspectionVibrationAlertEnabled = true
                    inspectionSoundAlertEnabled = true
                }
            }
        }

        if (!scrambleEnabled) {
            // CongratsText is by default aligned to below the scramble box. If it's missing, we have
            // to add an extra margin to account for the title header
            val params = binding.congratsText.layoutParams as MarginLayoutParams
            params.topMargin = ThemeUtils.dpToPix(
                requireContext(),
                70f
            ) // WARNING: this has to be the same as attr/actionBarPadding
            binding.congratsText.requestLayout()
        }

        if (!scrambleBackgroundEnabled) {
            binding.scrambleBox.root.setBackgroundColor(Color.TRANSPARENT)
            binding.scrambleBox.root.cardElevation = 0f
            binding.scrambleBox.scrambleText.setTextColor(
                ThemeUtils.fetchAttrColor(
                    requireContext(),
                    android.R.attr.colorPrimary
                )
            )
            binding.scrambleBox.scrambleButtonEdit.setColorFilter(
                ThemeUtils.fetchAttrColor(
                    requireContext(),
                    android.R.attr.colorPrimary
                )
            )
            binding.scrambleBox.scrambleButtonReset.setColorFilter(
                ThemeUtils.fetchAttrColor(
                    requireContext(),
                    android.R.attr.colorPrimary
                )
            )
            binding.scrambleBox.scrambleButtonHint.setColorFilter(
                ThemeUtils.fetchAttrColor(
                    requireContext(),
                    android.R.attr.colorPrimary
                )
            )
            binding.scrambleBox.scrambleButtonManualEntry.setColorFilter(
                ThemeUtils.fetchAttrColor(
                    requireContext(),
                    android.R.attr.colorPrimary
                )
            )
        }

        if (showHintsEnabled && currentPuzzle == TYPE_333 && scrambleEnabled) {
            binding.scrambleBox.scrambleButtonHint.visibility = View.VISIBLE
            optimalCross = RubiksCubeOptimalCross(getString(R.string.optimal_cross))
            optimalXCross = RubiksCubeOptimalXCross(getString(R.string.optimal_x_cross))
        }

        if (!scrambleEnabled) {
            binding.scrambleBox.root.visibility = View.GONE
            binding.scrambleImg.visibility = View.GONE
            isLocked = false
        }

        if (!scrambleImgEnabled) binding.scrambleImg.visibility = View.GONE
        if (!sessionStatsEnabled) {
            binding.sessionDetailTextAverage.visibility = View.INVISIBLE
            binding.sessionDetailTextOther.visibility = View.INVISIBLE
        }

        // Preferences //

        // Manual entry
        if (manualEntryEnabled) {
            binding.scrambleBox.scrambleButtonManualEntry.visibility = View.VISIBLE
            binding.scrambleBox.scrambleButtonManualEntry.setOnClickListener(buttonClickListener)
        }

        // Inspection timer
        if (inspectionEnabled) {
            if (inspectionAlertEnabled) {
                // If inspection time is 15 (the official WCA default), first warning should be
                // at 8 seconds in. Else, warn when half the time is up (8 is about 50% of 15)
                firstWarning =
                    CountdownWarning.Builder((if (inspectionTime == 15) 8 else (inspectionTime * 0.5f).toInt()).toLong())
                        .withVibrate(inspectionVibrationAlertEnabled)
                        .withTone(inspectionSoundAlertEnabled)
                        .toneCode(ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT)
                        .toneDuration(400)
                        .vibrateDuration(300)
                        .build()
                // If inspection time is default, warn at 12 seconds per competition rules, else,
                // warn at when 80% of the time is up (12 is 80% of 15)
                secondWarning =
                    CountdownWarning.Builder((if (inspectionTime == 15) 12 else (inspectionTime * 0.8f).toInt()).toLong())
                        .withVibrate(inspectionVibrationAlertEnabled)
                        .withTone(inspectionSoundAlertEnabled)
                        .toneCode(ToneGenerator.TONE_CDMA_NETWORK_BUSY)
                        .toneDuration(800)
                        .vibrateDuration(600)
                        .build()
            }
            countdown = object : CountDownTimer((inspectionTime * 1000).toLong(), 500) {
                override fun onTick(l: Long) {
                    val counter = (l / 1000) + 1
                    binding.chronometer.text = counter.toString()
                }

                override fun onFinish() {
                    binding.chronometer.let {
                        it.text = "+2"
                        // "+2" penalty is applied to "chronometer" when timer is eventually stopped.
                        currentPenalty = PENALTY_PLUSTWO
                        plusTwoCountdown?.start()
                    }
                }
            }

            plusTwoCountdown = object : CountDownTimer(2000, 500) {
                override fun onTick(l: Long) {
                    // The displayed value remains "+2" for the duration of this countdown.
                }

                override fun onFinish() {
                    // After counting down the inspection period, a "+2" penalty was counted down
                    // before the solve started, so this is a DNF. If the timer starts before this
                    // countdown ends, then "plusTwoCountdown" is canceled before this happens.
                    countingDown = false
                    isReady = false
                    holdingDNF = true
                    currentPenalty = PENALTY_DNF
                    binding.let {
                        it.chronometer.setPenalty(PENALTY_DNF)
                        stopChronometer()
                        addNewSolve()
                        it.inspectionText.visibility = View.GONE
                    }
                }
            }
        }

        // If hold-for-start is enabled, use the "isReady" flag to indicate if the hold was long
        // enough (0.5s) to trigger the starting of the timer.
        // Handled via holdJob in onTouch.

        binding.detailAverageRecordMessage.background =
            ThemeUtils.createSquareDrawableAttr(
                requireContext(),
                0,
                com.mikepenz.fastadapter.R.attr.colorPrimary,
                20,
                1.6f
            )

        // Chronometer
        binding.startTimerLayout.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
                if (!animationDone || isLocked && !isRunning) {
                    // Not ready to start the timer, yet. May be waiting on the animation of the
                    // restoration of the toolbars after the timer was stopped, or waiting on the
                    // generation of a scramble ("isLocked" flag).
                    // To compensate for long generating times, the timer generates a scramble
                    // while it is counting down. In this case, it's necessary to check if the timer
                    // is running, so the user can stop it.
                    return false
                }

                if (countingDown) {
                    // "countingDown == true" => "inspectionEnabled == true"
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // During inspection, touching down changes the text highlight color
                            // to indicate readiness to start timing. If the hold-for-start delay
                            // is enabled, that color change will be delayed. The timer will not
                            // start until the touch is lifted, but the inspection countdown
                            // will still continue in the meantime.
                            if (holdEnabled) {
                                isReady = false
                                holdJob?.cancel()
                                holdJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(HOLD_FOR_START_DELAY.milliseconds)
                                    isReady = true
                                    // Indicate to the user that the hold was long enough.
                                    binding.chronometer.setHighlighted(true)
                                    if (!inspectionEnabled) {
                                        // If inspection is enabled, the toolbar is already hidden.
                                        hideToolbar()
                                    }
                                }
                            } else if (startCueEnabled) {
                                binding.chronometer.setHighlighted(true)
                            }
                            // "chronometer.holdForStart" is not called here; it displays "0.00",
                            // which would interfere with the continuing countdown of the
                            // inspection
                            // period and, anyway, be overwritten by the next countdown "tick".
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            // Counting down inspection period. User has already touched down after
                            // starting the inspection, so start the timer unless "hold-to-start"
                            // is enabled and the hold delay was not long enough.
                            if (holdEnabled && !isReady) {
                                holdJob?.cancel()
                            } else {
                                stopInspectionCountdown()
                                startChronometer() // Toolbar is already hidden and remains so.
                            }
                            return false
                        }
                    }
                } else if (!isRunning) { // Not running and not counting down.
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (holdingDNF) {
                                holdingDNF = false
                                binding.chronometer.setHighlighted(false)
                            }

                            if (!inspectionEnabled) {
                                if (holdEnabled) {
                                    isReady = false
                                    holdJob?.cancel()
                                    holdJob = viewLifecycleOwner.lifecycleScope.launch {
                                        delay(HOLD_FOR_START_DELAY.milliseconds)
                                        isReady = true
                                        // Indicate to the user that the hold was long enough.
                                        binding.chronometer.setHighlighted(true)
                                        hideToolbar()
                                    }
                                } else if (startCueEnabled) {
                                    binding.chronometer.setHighlighted(true)
                                }
                                // Display "0.00" while holding in readiness for a new solve.
                                // This is not used above when inspection is enabled, as it would
                                // interfere with the countdown display.
                                binding.chronometer.holdForStart()
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (holdingDNF) {
                                // Checks if the user was holding the screen when the inspection
                                // timed out and saved a DNF
                                holdingDNF = false
                            } else if (inspectionEnabled) {
                                hideToolbar()
                                startInspectionCountdown(inspectionTime)
                            } else if (holdEnabled && !isReady) {
                                // Not held for long enough. Replace "0.00" with previous value.
                                binding.chronometer.cancelHoldForStart()
                                holdJob?.cancel()
                            } else {
                                // Inspection disabled. Hold-for-start disabled, or hold-for-start
                                // enabled, but the hold time was long enough. In the latter case,
                                // the toolbar will already have been hidden. Start timing!
                                if (!holdEnabled) {
                                    hideToolbar()
                                }
                                startChronometer()
                            }
                            return false
                        }
                    }
                } else if (motionEvent.action == MotionEvent.ACTION_DOWN
                    && binding.chronometer.elapsedTime >= 80
                ) { // => "isRunning == true"
                    // Chronometer is timing a solve (running, not counting down inspection period).
                    // Stop the timer if it has been running for long enough (80 ms) for this not to
                    // be an accidental touch as the user lifted the touch to start the timer.
                    animationDone = false
                    stopChronometer()
                    if (currentPenalty == PENALTY_PLUSTWO) {
                        // If a user has inspection on and went past his inspection time, he has
                        // two extra seconds do start his time, but with a +2 penalty. This penalty
                        // is recorded above (see plusTwoCountdown), and the timer checks if it's true here.
                        binding.chronometer.setPenalty(PENALTY_PLUSTWO)
                    }
                    addNewSolve()
                }
                return false
            }
        })
        onStatisticsUpdated(StatisticsCache.instance.statistics)
        StatisticsCache.instance.registerObserver(this) // Unregistered in "onDestroyView".
    }

    override fun onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()")
        super.onResume()
        if (scrambleEnabled) {
            if (realScramble == null) {
                generateNewScramble()
            } else {
                setScramble(realScramble)
            }
        }
    }

    /**
     * Stops the chronometer on back press.
     * 
     * @return
     * `true` if the "Back" button press was consumed to hide the scramble or stop the
     * timer; or `false` if neither was necessary and the "Back" button press was ignored.
     */
    override fun onBackPressedInFragment(): Boolean {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()")

        if (isResumed) {
            if (isRunning || countingDown) {
                cancelChronometer()
                return true
            }
        }
        return false
    }

    /**
     * Stops the inspection period countdown, and its warnings (if it is active). This cancels the
     * inspection countdown timer and associated "+2" countdown timer and hides the inspection text.
     */
    private fun stopInspectionCountdown() {
        // These timers may be null if inspection was not enabled when "updateLocale" was called.
        countdown?.cancel()
        plusTwoCountdown?.cancel()
        firstWarning?.cancel()
        secondWarning?.cancel()

        binding?.inspectionText?.visibility = View.GONE
        countingDown = false
    }

    /**
     * Starts the inspection period countdown.
     * 
     * @param inspectionTime
     * The inspection time in seconds.
     */
    private fun startInspectionCountdown(inspectionTime: Int) {
        // The "countdown" timer may be null if inspection was not enabled when "updateLocale" was
        // called. In that case this method will not be called from the touch listener.

        // So it doesn't flash the old time when the inspection starts

        binding?.let {
            it.chronometer.text = inspectionTime.toString()
            it.inspectionText.visibility = View.VISIBLE
        }
        countdown?.start()
        firstWarning?.start()
        secondWarning?.start()
        countingDown = true
    }

    /**
     * Calculates scramble image height multiplier to respect aspect ratio
     * 
     * @param multiplier the height multiplier (must be the same multiplier as the width)
     * 
     * @return the height in px
     */
    private fun calculateScrambleImageHeightMultiplier(multiplier: Float): Float {
        when (currentPuzzle) {
            PuzzleUtils.TYPE_777, PuzzleUtils.TYPE_666, PuzzleUtils.TYPE_555, PuzzleUtils.TYPE_222, PuzzleUtils.TYPE_444, TYPE_333 ->                 // 3 faces of the cube vertically divided by 4 faces horizontally (it draws the cube like a cross)
                return (multiplier / 4) * 3

            PuzzleUtils.TYPE_CLOCK -> return multiplier / 2
            PuzzleUtils.TYPE_MEGA -> return (multiplier / 2)
            PuzzleUtils.TYPE_PYRA ->
                // Just Pythagoras. Height of an equilateral triangle
                return (multiplier / sqrt(1.25)).toFloat()

            PuzzleUtils.TYPE_SKEWB ->
                // This one is the same as the NxN cubes
                return (multiplier / 4) * 3

            PuzzleUtils.TYPE_SQUARE1 -> return multiplier
        }
        return multiplier
    }

    private fun addNewSolve() {
        val binding = binding ?: return
        val solve = Solve(
            binding.chronometer.elapsedTime.toInt(),  // Includes any "+2" penalty. Is zero for "DNF".
            currentPuzzle ?: "", currentPuzzleCategory ?: "",
            System.currentTimeMillis(), currentScramble ?: "", currentPenalty, "", false,
            currentModeInt
        )
        currentSolve = solve

        if (currentPenalty != PENALTY_DNF) {
            declareRecordTimes(solve)
        }

        lifecycleScope.launch {
            val id = TwistyTimer.getSolveRepository().insertSolve(
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
            solve.id = id
            currentPenalty = NO_PENALTY
        }
    }

    private fun broadcastNewSolve() {
        // The receiver might be able to use the new solve and avoid accessing the database, so
        // parcel it up in the intent.
        BroadcastBuilder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED)
            .solve(currentSolve)
            .broadcast()
    }

    /**
     * Declares a new all-time best or worst solve time, if the new solve time sets a record. The
     * first valid solve time will not set any records; it is itself the best and worst time and
     * only later times will be compared to it. If the solve time is not greater than zero, or if
     * the solve is a DNF, the solve will be ignored and no new records will be declared.
     * 
     * @param solve The solve (time) to be tested.
     */
    private fun declareRecordTimes(solve: Solve) {
        // NOTE: The old approach did not check for PB/record solves until at least 4 previous
        // solves had been recorded for the *current session*. This seemed a bit arbitrary. Perhaps
        // it had to do with waiting for the best and worst times to be loaded. If a user records
        // their *first* solve for the current session, and it beats the best time from *any* past
        // session, it should be reported *immediately*, not ignored just because the session has
        // only started. However, the limit should perhaps have been 4 previous solves in the full
        // history of all past and current sessions. If this is the first ever session, then it
        // would be annoying if each of the first few times were reported as a record of some sort.
        // Therefore, do not report PB records until at least 4 previous *non-DNF* times have been
        // recorded in the database across all sessions, including the current session.

        val newTime = solve.time.toLong()

        if (solve.penalty == PENALTY_DNF || newTime <= 0 || mRecentStatistics == null || (mRecentStatistics!!.allTimeNumSolves
                    - mRecentStatistics!!.allTimeNumDNFSolves < 4)
        ) {
            // Not a valid time, or there are no previous statistics, or not enough previous times
            // to make reporting meaningful (or non-annoying), so cannot check for a new PB.
            return
        }

        if (bestSolveEnabled) {
            val previousBestTime = mRecentStatistics?.allTimeBestTime ?: AverageCalculator.UNKNOWN

            // If "previousBestTime" is a DNF or UNKNOWN, it will be less than zero, so the new
            // solve time cannot better (i.e., lower).
            if (previousBestTime > 0 && newTime < previousBestTime) {
                binding?.rippleBackground?.startRippleAnimation()
                binding?.congratsText?.text = getString(
                    R.string.personal_best_message,
                    convertTimeToString(previousBestTime - newTime, FORMAT_DEFAULT)
                )
                binding?.congratsText?.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    delay(2900.milliseconds)
                    binding?.rippleBackground?.stopRippleAnimation()
                }
            }
        }

        if (worstSolveEnabled) {
            val previousWorstTime = mRecentStatistics?.allTimeWorstTime ?: AverageCalculator.UNKNOWN
            val poopDrawable = ThemeUtils.tintDrawable(
                requireContext(),
                R.drawable.ic_emoticon_poop,
                android.R.attr.colorPrimary
            )
            // If "previousWorstTime" is a DNF or UNKNOWN, it will be less than zero. Therefore,
            // make sure it is at least greater than zero before testing against the new time.
            if (previousWorstTime in 1..<newTime) {
                binding?.congratsText?.text = getString(
                    R.string.personal_worst_message,
                    convertTimeToString(newTime - previousWorstTime, FORMAT_DEFAULT)
                )

                binding?.congratsText?.setCompoundDrawablesWithIntrinsicBounds(
                    poopDrawable, null,
                    poopDrawable, null
                )

                binding?.congratsText?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view, or if the given
     * statistics are `null`, no update will be attempted.
     * 
     * @param stats
     * The updated statistics. These will not be modified.
     */
    @SuppressLint("SetTextI18n")
    override fun onStatisticsUpdated(stats: Statistics?) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated($stats)")

        if (view == null || !sessionStatsEnabled) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return
        }

        // Save these for later. The best and worst times can be retrieved and compared to the next
        // new solve time to be added via "addNewSolve".
        mRecentStatistics = stats // May be null.

        if (stats == null) {
            return
        }

        val sessionDeviation = convertTimeToString(
            tr(stats.sessionStdDeviation), FORMAT_DEFAULT
        )
        val sessionCount = String.format(Locale.getDefault(), "%,d", stats.sessionNumSolves)
        val sessionBestTime =
            convertTimeToString(tr(stats.sessionBestTime), FORMAT_DEFAULT)
        val sessionMean = convertTimeToString(tr(stats.sessionMeanTime), FORMAT_DEFAULT)

        val allTimeBestAvg = LongArray(4)
        val sessionCurrentAvg = LongArray(4)

        allTimeBestAvg[0] =
            tr(stats.getAverageOf(5, false)?.bestAverage ?: AverageCalculator.UNKNOWN)
        allTimeBestAvg[1] =
            tr(stats.getAverageOf(12, false)?.bestAverage ?: AverageCalculator.UNKNOWN)
        allTimeBestAvg[2] =
            tr(stats.getAverageOf(50, false)?.bestAverage ?: AverageCalculator.UNKNOWN)
        allTimeBestAvg[3] =
            tr(stats.getAverageOf(100, false)?.bestAverage ?: AverageCalculator.UNKNOWN)

        sessionCurrentAvg[0] =
            tr(stats.getAverageOf(5, true)?.currentAverage ?: AverageCalculator.UNKNOWN)
        sessionCurrentAvg[1] =
            tr(stats.getAverageOf(12, true)?.currentAverage ?: AverageCalculator.UNKNOWN)
        sessionCurrentAvg[2] =
            tr(stats.getAverageOf(50, true)?.currentAverage ?: AverageCalculator.UNKNOWN)
        sessionCurrentAvg[3] =
            tr(stats.getAverageOf(100, true)?.currentAverage ?: AverageCalculator.UNKNOWN)

        // detailTextNamesArray should be in the same order as shown in the timer
        // (keep R.arrays.timer_detail_stats in sync with the order!)
        val stringDetailOther = StringBuilder()
        detailTextNamesArray.let { names ->
            stringDetailOther.append(names[4]).append(": ").append(sessionDeviation)
                .append("\n")
            stringDetailOther.append(names[5]).append(": ").append(sessionMean)
                .append("\n")
            stringDetailOther.append(names[6]).append(": ").append(sessionBestTime)
                .append("\n")
            stringDetailOther.append(names[7]).append(": ").append(sessionCount)
        }

        binding?.sessionDetailTextOther?.text = stringDetailOther.toString()

        // To prevent the record message being animated more than once in case the user sets
        // two or more average records at the same time.
        var hasShownRecordMessage = false

        // reset card visibility
        binding?.detailAverageRecordMessage?.visibility = View.GONE

        val stringDetailAvg = StringBuilder()

        // Iterate through averages and set respective TextViews
        val avgNumbers = arrayOf<String?>("5", "12", "50", "100")
        for (i in 0..3) {
            if (sessionStatsEnabled && averageRecordsEnabled && hasStoppedTimerOnce && sessionCurrentAvg[i] > 0 && sessionCurrentAvg[i] <= allTimeBestAvg[i]) {
                // Create string.
                detailTextNamesArray.let { names ->
                    stringDetailAvg.append("<u><b>").append(names[i])
                        .append(avgNumbers[i]).append(": ")
                        .append(convertTimeToString(sessionCurrentAvg[i], FORMAT_DEFAULT))
                        .append("</b></u>")
                }

                // Show record message, if it was not shown before
                if (!hasShownRecordMessage && !isRunning && !countingDown) {
                    binding?.let { b ->
                        b.detailAverageRecordMessage.visibility = View.VISIBLE
                        b.detailAverageRecordMessage
                            .animate()
                            .alpha(1f)
                            .setDuration(mAnimationDuration.toLong())
                    }
                    hasShownRecordMessage = true
                }
            } else if (sessionStatsEnabled) {
                detailTextNamesArray.let { names ->
                    stringDetailAvg.append(names[i]).append(avgNumbers[i]).append(": ")
                        .append(convertTimeToString(sessionCurrentAvg[i], FORMAT_DEFAULT))
                }
            }
            // append newline to every line but the last
            if (i < 3) {
                stringDetailAvg.append("<br>")
            }
        }

        binding?.sessionDetailTextAverage?.text =
            HtmlCompat.fromHtml(
                stringDetailAvg.toString(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

        if (!isRunning && !countingDown) showDetailStats()
    }

    private fun generateScrambleImage() {
        lifecycleScope.launch {
            val drawable = withContext(Dispatchers.IO) {
                generator?.generateImageFromScramble(
                    PreferenceManager.getDefaultSharedPreferences(requireContext()),
                    realScramble
                )
            }
            if (!isRunning) {
                if (binding?.scrambleImg != null) showImage()
            }
            binding?.let {
                it.progressSpinner.visibility = View.INVISIBLE
                it.scrambleImg.setImageDrawable(drawable)
                it.expandedImage.setImageDrawable(drawable)
            }
        }
    }

    private fun showToolbar() {
        unlockOrientation(requireActivity())
        // Resize startTimerLayout to have the little margin
        // on the left that allows the user to open the side menu.
        val params = binding?.startTimerLayout?.layoutParams as? MarginLayoutParams
        params?.leftMargin = ThemeUtils.dpToPix(requireContext(), 16f)
        binding?.startTimerLayout?.requestLayout()
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STOPPED)
    }

    private fun showItems() {
        // reset chronometer position

        binding?.chronometer?.animate()
            ?.scaleX(1f)
            ?.scaleY(1f)?.duration = mAnimationDuration.toLong()
        binding?.inspectionText?.animate()
            ?.translationY(0f)?.duration = mAnimationDuration.toLong()

        if (scrambleEnabled) {
            binding?.scrambleBox?.root?.visibility = View.VISIBLE
            binding?.scrambleBox?.root?.animate()
                ?.alpha(1f)
                ?.translationY(0f)?.duration = mAnimationDuration.toLong()
            binding?.scrambleBox?.root?.isEnabled = true
            if (scrambleImgEnabled) {
                binding?.scrambleImg?.isEnabled = true
                showImage()
            }
        }
        if (buttonsEnabled && !isCanceled) {
            binding?.qaButtons?.qaLayout?.isEnabled = true
            binding?.qaButtons?.qaLayout?.visibility = View.VISIBLE
            binding?.qaButtons?.qaLayout?.animate()
                ?.alpha(.9f)?.duration = mAnimationDuration.toLong()
        }
    }

    private fun showDetailStats() {
        binding?.sessionDetailTextAverage?.visibility = View.VISIBLE
        binding?.sessionDetailTextAverage?.animate()
            ?.alpha(1f)
            ?.translationY(0f)?.duration = mAnimationDuration.toLong()

        binding?.sessionDetailTextOther?.visibility = View.VISIBLE
        binding?.sessionDetailTextOther?.animate()
            ?.alpha(1f)
            ?.translationY(0f)?.duration = mAnimationDuration.toLong()
    }

    private fun showImage() {
        binding?.scrambleImg?.visibility = View.VISIBLE
        binding?.scrambleImg?.isEnabled = true
        binding?.scrambleImg?.animate()
            ?.alpha(1f)
            ?.translationY(0f)?.duration = mAnimationDuration.toLong()
    }

    private fun hideImage() {
        binding?.scrambleImg?.animate()
            ?.alpha(0f)
            ?.translationY(binding?.scrambleImg?.height?.toFloat() ?: 0f)
            ?.setDuration(mAnimationDuration.toLong())
            ?.withEndAction {
                binding?.scrambleImg?.visibility = View.GONE
                binding?.scrambleImg?.isEnabled = false
            }
    }

    private fun hideToolbar() {
        lockOrientation(requireActivity())
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STARTED)

        binding?.congratsText?.visibility = View.GONE
        binding?.congratsText?.setCompoundDrawables(null, null, null, null)

        // bring chronometer up a bit
        binding?.chronometer?.animate()
            ?.scaleX(1.15f)
            ?.scaleY(1.15f)?.duration = mAnimationDuration.toLong()
        binding?.inspectionText?.animate()
            ?.translationY(-actionBarSize.toFloat())?.duration = mAnimationDuration.toLong()

        // Resize startTimerLayout to fill the entire screen. This removes the little margin
        // on the left that allows the user to open the side menu.
        val params = binding?.startTimerLayout?.layoutParams as? MarginLayoutParams
        params?.leftMargin = 0
        binding?.startTimerLayout?.requestLayout()

        if (scrambleEnabled) {
            binding?.scrambleBox?.root?.isEnabled = false
            binding?.scrambleBox?.root?.animate()
                ?.alpha(0f)
                ?.translationY(-(binding?.scrambleBox?.root?.height?.toFloat() ?: 0f))
                ?.setDuration(mAnimationDuration.toLong())
                ?.withEndAction {
                    binding?.scrambleBox?.root?.visibility = View.INVISIBLE
                }
            if (scrambleImgEnabled) {
                binding?.scrambleImg?.isEnabled = false
                hideImage()
            }
        }
        if (sessionStatsEnabled) {
            binding?.sessionDetailTextAverage?.animate()
                ?.alpha(0f)
                ?.translationY(binding?.sessionDetailTextAverage?.height?.toFloat() ?: 0f)
                ?.setDuration(mAnimationDuration.toLong())
                ?.withEndAction {
                    binding?.sessionDetailTextAverage?.visibility = View.INVISIBLE
                }
            binding?.sessionDetailTextOther?.animate()
                ?.alpha(0f)
                ?.translationY(binding?.sessionDetailTextOther?.height?.toFloat() ?: 0f)
                ?.setDuration(mAnimationDuration.toLong())
                ?.withEndAction {
                    binding?.sessionDetailTextOther?.visibility = View.INVISIBLE
                }
        }
        if (buttonsEnabled) {
            binding?.qaUndo?.visibility = View.GONE
            binding?.qaButtons?.qaLayout?.isEnabled = false
            binding?.qaButtons?.qaLayout?.animate()
                ?.alpha(0f)
                ?.setDuration(mAnimationDuration.toLong())
                ?.withEndAction {
                    binding?.qaButtons?.qaLayout?.visibility = View.GONE
                }
        }
        if (averageRecordsEnabled) {
            binding?.detailAverageRecordMessage?.animate()
                ?.alpha(0f)
                ?.setDuration(mAnimationDuration.toLong())
                ?.withEndAction {
                    binding?.detailAverageRecordMessage?.visibility = View.GONE
                }
        }
    }

    /**
     * Starts the chronometer from zero and removes any color highlight.
     */
    private fun startChronometer() {
        binding?.let {
            it.chronometer.reset() // Start from "0.00"; do not resume from the previous time.
            it.chronometer.start()
            it.chronometer.setHighlighted(false) // Clear any start cue or hold-for-start highlight.
        }

        // isRunning should be set before generateNewScramble so the loading spinner doesn't appear
        // during a solve, since generateNewScramble checks if isRunning is false before setting
        // the spinner to visible.
        isRunning = true

        if (scrambleEnabled) {
            currentScramble = realScramble
            generateNewScramble()
        }
    }

    /**
     * Stops the chronometer
     */
    private fun stopChronometer() {
        binding?.let {
            it.chronometer.stop()
            it.chronometer.setHighlighted(false)
        }
        isRunning = false
        hasStoppedTimerOnce = true
        showToolbar()
    }

    /**
     * Cancels the chronometer and any inspection countdown. Nothing is saved and the timer is
     * reset to zero.
     */
    private fun cancelChronometer() {
        if (backCancelEnabled) {
            stopInspectionCountdown()
            stopChronometer()

            binding?.chronometer?.reset() // Show "0.00".
            isCanceled = true
            currentPenalty = NO_PENALTY
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SCRAMBLE, realScramble)
        outState.putString(PUZZLE, currentPuzzle)
        outState.putString(TIMER_MODE, currentTimerMode)
        outState.putBoolean(HAS_STOPPED_TIMER_ONCE, hasStoppedTimerOnce)
    }

    override fun onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()")
        super.onDetach()
        // To fix memory leaks
        scrambleJob?.cancel()
        optimalCrossJob?.cancel()
    }

    override fun onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
        binding = null
        StatisticsCache.instance.unregisterObserver(this)
        mRecentStatistics = null
    }

    val newOptimalCross: Unit
        get() {
            if (showHintsEnabled) {
                optimalCrossJob?.cancel()
                val scramble = realScramble ?: return
                val cross = optimalCross ?: return
                val cross2 = optimalXCross ?: return
                optimalCrossJob = lifecycleScope.launch {
                    val text = withContext(Dispatchers.Default) {
                        var t = ""
                        t += cross.getTip(scramble)
                        if (showHintsXCrossEnabled) {
                            t += "\n\n"
                            t += cross2.getTip(scramble)
                        }
                        t
                    }
                    if (!isRunning) {
                        // Set the hint text
                        if (scrambleDialog != null) {
                            scrambleDialog!!.setHintText(text)
                            scrambleDialog!!.setHintVisibility(View.VISIBLE)
                        }
                    }
                }
            }
        }

    /**
     * Generates a new scramble and handles everything.
     */
    private fun generateNewScramble() {
        if (scrambleEnabled && currentTimerMode == TIMER_MODE_TIMER) {
            scrambleJob?.cancel()
            scrambleJob = lifecycleScope.launch {
                if (showHintsEnabled && currentPuzzle == TYPE_333 && scrambleEnabled && scrambleDialog != null) {
                    scrambleDialog?.setHintVisibility(View.GONE)
                    scrambleDialog?.dismiss()
                }
                canShowHint = false
                binding?.let {
                    it.scrambleBox.scrambleText.setText(R.string.generating_scramble)
                    it.scrambleBox.scrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    it.scrambleBox.scrambleText.isClickable = false

                    it.scrambleBox.scrambleButtonHint.visibility = View.GONE
                    it.scrambleBox.scrambleButtonEdit.visibility = View.GONE
                    it.scrambleBox.scrambleButtonReset.visibility = View.GONE
                    it.scrambleBox.scrambleButtonManualEntry.visibility = View.GONE
                    it.scrambleBox.scrambleProgress.visibility = View.VISIBLE
                }

                hideImage()
                if (!isRunning) binding?.progressSpinner?.visibility = View.VISIBLE
                isLocked = true

                val scramble = withContext(Dispatchers.Default) {
                    try {
                        generator?.puzzle?.generateScramble()
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid puzzle for generator: $e")
                        "An error has occurred"
                    }
                }
                setScramble(scramble)
            }
        } else if (currentTimerMode == TIMER_MODE_TRAINER) {
            setScramble(
                TrainerScrambler.generateTrainerCase(
                    requireContext(),
                    currentSubset ?: return,
                    currentPuzzleCategory
                )
            )
            canShowHint = false
            hideButtons(hideQuickActionButtons = true, hideUndoButton = true)
        }
    }

    /**
     * Updates everything related to displaying the current scramble
     * Ex. scramble image, box, text, dialogs
     */
    private fun setScramble(scramble: String?) {
        val binding = binding ?: return
        realScramble = scramble
        binding.scrambleBox.scrambleText.text = scramble
        binding.scrambleBox.scrambleText.post {
            binding.chronometer.post {
                if (this.binding != null && this.binding?.scrambleBox?.scrambleText != null) {
                    // Calculate surrounding layouts to make sure the scramble text doesn't intersect any element
                    // If it does, show only a "tap here to see more" hint instead of the scramble
                    val scrambleRect = Rect(
                        binding.scrambleBox.root.left,
                        binding.scrambleBox.root.top,
                        binding.scrambleBox.root.right,
                        binding.scrambleBox.root.bottom
                    )
                    // The top line calculation is a bit tricky
                    // We first get the top of the bounding box which isn't necessarily
                    // the top of the actual, visible text. To that, we add the baseline,
                    // which is the measure from the top of the box to the actual baseline
                    // of the text. Then, we add the text size, which gets us to the visible
                    // top.
                    val chronometerRect = Rect(
                        binding.chronometer.left,
                        (binding.chronometer.top
                                + binding.chronometer.baseline
                                - binding.chronometer.textSize
                                + ThemeUtils.dpToPix(requireContext(), 28f)).toInt(),
                        binding.chronometer.right,
                        binding.chronometer.bottom
                    )
                    val congratsRect = Rect(
                        binding.congratsText.left,
                        binding.congratsText.top,
                        binding.congratsText.right,
                        binding.congratsText.bottom
                    )

                    if ((Rect.intersects(scrambleRect, chronometerRect)) ||
                        (binding.congratsText.isVisible && Rect.intersects(
                            chronometerRect,
                            congratsRect
                        ))
                    ) {
                        val hintText = "[ " + getString(R.string.scramble_text_tap_hint) + " ]"
                        binding.scrambleBox.scrambleText.text = hintText
                        binding.scrambleBox.root.isClickable = true
                        binding.scrambleBox.root.setOnClickListener(scrambleDetailClickListener)
                    } else {
                        binding.scrambleBox.root.setOnClickListener(null)
                        binding.scrambleBox.root.isClickable = false
                        binding.scrambleBox.root.isFocusable = false
                    }
                    binding.scrambleBox.scrambleButtonHint.setOnClickListener(
                        scrambleDetailClickListener
                    )
                }
            }
        }

        if (showHintsEnabled && currentPuzzle == TYPE_333) binding.scrambleBox.scrambleButtonHint.visibility =
            View.VISIBLE
        if (manualEntryEnabled) binding.scrambleBox.scrambleButtonManualEntry.visibility =
            View.VISIBLE
        binding.scrambleBox.scrambleProgress.visibility = View.GONE
        binding.scrambleBox.scrambleButtonEdit.visibility = View.VISIBLE
        binding.scrambleBox.scrambleButtonReset.visibility = View.VISIBLE

        if (scrambleImgEnabled) generateScrambleImage()
        else binding.progressSpinner.visibility = View.INVISIBLE
        isLocked = false

        if (showHintsEnabled) canShowHint = true

        // Broadcast the new scramble
        BroadcastBuilder(CATEGORY_UI_INTERACTIONS, ACTION_SCRAMBLE_MODIFIED)
            .scramble(realScramble)
            .broadcast()
    }

    private val scrambleDetailClickListener: View.OnClickListener = View.OnClickListener {
        scrambleDialog = BottomSheetDetailDialog()
        scrambleDialog!!.setDetailText(realScramble)
        scrambleDialog!!.setDetailTextSize(scrambleTextSize)
        if (canShowHint && showHintsEnabled && currentPuzzle == TYPE_333) {
            newOptimalCross
            scrambleDialog!!.hasHints(true)
        }
        if (mFragManager != null) scrambleDialog!!.show(
            mFragManager!!,
            "fragment_dialog_scramble_detail"
        )
    }

    private fun zoomImageFromThumb(thumbView: View) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        mCurrentAnimator?.cancel()

        val binding = binding ?: return

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBounds = Rect()
        val finalBounds = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds)
        binding.root.getGlobalVisibleRect(finalBounds, globalOffset)
        startBounds.offset(-globalOffset.x, -globalOffset.y)
        globalOffset.y -= binding.scrambleBox.root.height
        finalBounds.offset(-globalOffset.x, -globalOffset.y)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if (finalBounds.width().toFloat() / finalBounds.height()
            > startBounds.width().toFloat() / startBounds.height()
        ) {
            // Extend start bounds horizontally
            startScale = startBounds.height().toFloat() / finalBounds.height()
            val startWidth = startScale * finalBounds.width()
            val deltaWidth = (startWidth - startBounds.width()) / 2
            startBounds.left = (startBounds.left - deltaWidth).toInt()
            startBounds.right = (startBounds.right + deltaWidth).toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width().toFloat() / finalBounds.width()
            val startHeight = startScale * finalBounds.width()
            val deltaHeight = (startHeight - startBounds.height()) / 2
            startBounds.top = (startBounds.top - deltaHeight).toInt()
            startBounds.bottom = (startBounds.bottom + deltaHeight).toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.alpha = 0f
        binding.expandedImage.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        binding.expandedImage.pivotX = 0f
        binding.expandedImage.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        val set = AnimatorSet()
        set
            .play(
                ObjectAnimator.ofFloat(
                    binding.expandedImage, View.X,
                    startBounds.left.toFloat(), finalBounds.left.toFloat()
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    binding.expandedImage, View.Y,
                    startBounds.top.toFloat(), finalBounds.top.toFloat()
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    binding.expandedImage, View.SCALE_X,
                    startScale, 1f
                )
            ).with(
                ObjectAnimator.ofFloat(
                    binding.expandedImage,
                    View.SCALE_Y, startScale, 1f
                )
            )
        set.duration = mAnimationDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mCurrentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                mCurrentAnimator = null
            }
        })
        set.start()
        mCurrentAnimator = set

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        val startScaleFinal = startScale
        binding.expandedImage.setOnClickListener { _: View? ->
            mCurrentAnimator?.cancel()
            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            val set1 = AnimatorSet()
            set1.play(
                ObjectAnimator
                    .ofFloat(binding.expandedImage, View.X, startBounds.left.toFloat())
            )
                .with(
                    ObjectAnimator
                        .ofFloat(
                            binding.expandedImage,
                            View.Y, startBounds.top.toFloat()
                        )
                )
                .with(
                    ObjectAnimator
                        .ofFloat(
                            binding.expandedImage,
                            View.SCALE_X, startScaleFinal
                        )
                )
                .with(
                    ObjectAnimator
                        .ofFloat(
                            binding.expandedImage,
                            View.SCALE_Y, startScaleFinal
                        )
                )
            set1.duration = mAnimationDuration.toLong()
            set1.interpolator = DecelerateInterpolator()
            set1.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    thumbView.alpha = 1f
                    binding.expandedImage.visibility = View.GONE
                    mCurrentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    thumbView.alpha = 1f
                    binding.expandedImage.visibility = View.GONE
                    mCurrentAnimator = null
                }
            })
            set1.start()
            mCurrentAnimator = set1
        }
    }

    companion object {
        // Specifies the timer mode
        // i.e: Trainer mode generates only trainer scrambles, and changes the puzzle select spinner
        // Can be used for other features in the future
        const val TIMER_MODE_TIMER: String = "TIMER_MODE_TIMER"
        const val TIMER_MODE_TRAINER: String = "TIMER_MODE_TRAINER"

        /**
         * Flag to enable debug logging for this class.
         */
        private const val DEBUG_ME = true

        /**
         * A "tag" to identify this class in log messages.
         */
        private val TAG: String = TimerFragment::class.java.simpleName

        private const val PUZZLE = "puzzle"
        private const val PUZZLE_SUBTYPE = "puzzle_type"
        private const val TRAINER_SUBSET = "trainer_subset"
        private const val TIMER_MODE = "timer_mode"
        private const val SCRAMBLE = "scramble"
        private const val HAS_STOPPED_TIMER_ONCE = "has_stopped_timer_once"


        /**
         * The time delay in milliseconds before starting the chronometer if the hold-for-start
         * preference is set.
         */
        private const val HOLD_FOR_START_DELAY = 500L

        fun newInstance(
            puzzle: String?,
            puzzleSubType: String?,
            timerMode: String?,
            subset: TrainerSubset?
        ): TimerFragment {
            val fragment = TimerFragment()
            val args = Bundle()
            args.putString(PUZZLE, puzzle)
            args.putString(PUZZLE_SUBTYPE, puzzleSubType)
            args.putString(TIMER_MODE, timerMode)
            args.putSerializable(TRAINER_SUBSET, subset)
            fragment.setArguments(args)
            if (DEBUG_ME) Log.d(TAG, "newInstance() -> $fragment")
            return fragment
        }

        private fun lockOrientation(activity: Activity) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }

        private fun unlockOrientation(activity: Activity) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        @JvmStatic
        fun modeToInt(mode: String?): Int {
            return if (mode == TIMER_MODE_TRAINER) 1 else 0
        }
    }
}
