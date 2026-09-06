package com.aricneto.twistytimer.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.FragmentTimeListBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.adapter.SolveListAdapter
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.fragment.dialog.AddTimeDialog
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.stats.StatisticsCache
import com.aricneto.twistytimer.stats.StatisticsCache.StatisticsObserver
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTIntent.ACTION_COMMENT_ADDED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_DELETE_SELECTED_TIMES
import com.aricneto.twistytimer.utils.TTIntent.ACTION_HISTORY_TIMES_SHOWN
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SCRAMBLE_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_OFF
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SESSION_TIMES_SHOWN
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MOVED_TO_HISTORY
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.TTIntent.getScramble
import com.aricneto.twistytimer.utils.TTIntent.registerReceiver
import com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.viewmodel.TimerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.util.Random


class TimerListFragment : BaseFragment(), OnBackPressedInFragmentListener, StatisticsObserver {
    private var mContext: Context? = null

    private val viewModel: TimerViewModel by lazy {
        ViewModelProvider(this)[TimerViewModel::class.java]
    }

    // True if you want to search history, false if you only want to search session
    var history: Boolean = false
    var mode: Int = 0

    private var binding: FragmentTimeListBinding? = null

    private var currentPuzzle: String? = null
    private var currentPuzzleCategory: String? = null
    private var currentScramble: String? = null

    private var orderByKey = SolveRepository.KEY_DATE
    private var orderByDir = SolveRepository.DIR_DESC

    // Stores the current comment search query
    private var searchComment = ""

    private var solveListAdapter: SolveListAdapter? = null

    /**
     * The most recently notified solve time statistics. These may be used when sharing averages.
     */
    private var mRecentStatistics: Statistics? = null

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.add_time_button -> {
                val addTimeDialog = AddTimeDialog.newInstance(
                    currentPuzzle,
                    currentPuzzleCategory,
                    currentScramble,
                    mode
                )
                val manager = parentFragmentManager
                addTimeDialog.show(manager, "dialog_add_time")
            }

            R.id.archive_button -> {
                val text: Spannable =
                    SpannableString(getString(R.string.move_solves_to_history_content) + "  ")
                text.setSpan(
                    ThemeUtils.getIconSpan(mContext!!, 0.6f),
                    text.length - 1,
                    text.length,
                    0
                )

                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.move_solves_to_history)
                    .setMessage(text)
                    .setPositiveButton(
                        R.string.action_move
                    ) { _: DialogInterface?, _: Int ->
                        lifecycleScope.launch {
                            TwistyTimer.getSolveRepository().moveAllSolvesToHistory(
                                currentPuzzle!!, currentPuzzleCategory!!, mode
                            )
                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MOVED_TO_HISTORY)
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.clear_button -> MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.remove_session_title)
                .setMessage(R.string.remove_session_confirmation_content)
                .setPositiveButton(
                    R.string.action_remove
                ) { _: DialogInterface?, _: Int ->
                    lifecycleScope.launch {
                        TwistyTimer.getSolveRepository().deleteAllFromSession(
                            currentPuzzle!!, currentPuzzleCategory!!, mode
                        )
                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()

            R.id.more_button -> {
                // Main popup
                val popupMenu = PopupMenu(requireActivity(), binding!!.moreButton)
                popupMenu.menuInflater.inflate(R.menu.menu_list_more, popupMenu.menu)

                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.unarchive -> {
                            lifecycleScope.launch {
                                val unarchiveView = LayoutInflater.from(mContext)
                                    .inflate(
                                        R.layout.dialog_input,
                                        requireView().parent as ViewGroup,
                                        false
                                    )
                                val unarchiveEditText =
                                    unarchiveView.findViewById<TextInputEditText>(R.id.edit_text)
                                unarchiveEditText.inputType = InputType.TYPE_CLASS_NUMBER

                                val numArchived =
                                    TwistyTimer.getSolveRepository().getNumArchivedSolves(
                                        currentPuzzle!!,
                                        currentPuzzleCategory!!,
                                        mode
                                    )

                                MaterialAlertDialogBuilder(requireActivity())
                                    .setTitle(R.string.list_options_item_from_history)
                                    .setMessage(
                                        getString(
                                            R.string.unarchive_dialog_summary,
                                            numArchived
                                        )
                                    )
                                    .setView(unarchiveView)
                                    .setPositiveButton(
                                        R.string.list_options_item_from_history
                                    ) { _: DialogInterface?, _: Int ->
                                        lifecycleScope.launch {
                                            try {
                                                TwistyTimer.getSolveRepository().unarchiveSolves(
                                                    currentPuzzle!!,
                                                    currentPuzzleCategory!!,
                                                    mode,
                                                    unarchiveEditText.getText().toString().toLong()
                                                )
                                                broadcast(
                                                    CATEGORY_TIME_DATA_CHANGES,
                                                    ACTION_TIME_ADDED
                                                )
                                            } catch (_: NumberFormatException) {
                                                // ignore
                                            }
                                        }
                                    }
                                    .setNegativeButton(R.string.action_cancel, null)
                                    .show()
                            }
                        }

                        R.id.share_ao5 -> PuzzleUtils.shareAverageOf(
                            5,
                            currentPuzzle,
                            mRecentStatistics,
                            requireActivity()
                        )

                        R.id.share_ao12 -> PuzzleUtils.shareAverageOf(
                            12,
                            currentPuzzle,
                            mRecentStatistics,
                            requireActivity()
                        )

                        R.id.share_histogram -> PuzzleUtils.shareHistogramOf(
                            currentPuzzle,
                            mRecentStatistics,
                            requireActivity()
                        )

                        R.id.sort_time -> orderByKey = SolveRepository.KEY_TIME
                        R.id.sort_date -> orderByKey = SolveRepository.KEY_DATE
                        R.id.sort_ascd, R.id.sort_asc -> {
                            orderByDir = SolveRepository.DIR_ASC
                            reloadList()
                        }

                        R.id.sort_descd, R.id.sort_desc -> {
                            orderByDir = SolveRepository.DIR_DESC
                            reloadList()
                        }

                        else -> {}
                    }
                    true
                }

                try {
                    val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                    fieldPopup.isAccessible = true
                    val menuPopupHelper = fieldPopup.get(popupMenu)
                    val setForceIcons = menuPopupHelper.javaClass
                        .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                popupMenu.show()
            }
        }
    }

    // Receives broadcasts after changes have been made to time data or the selection of that data.
    private val mTimeDataChangedReceiver
            : TTFragmentBroadcastReceiver =
        object : TTFragmentBroadcastReceiver(this, CATEGORY_TIME_DATA_CHANGES) {
            override fun onReceiveWhileAdded(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_COMMENT_ADDED -> if (!history) reloadList()
                    ACTION_TIME_ADDED ->
                        // When "history" is enabled, the list of times does not include times from
                        // the current session. Times are only added to the current session, so
                        // there is no need to refresh the "history" list on adding a session time.
                        if (!history) {
                            /*
                            If a time has been added by the timer, wait a few seconds to let the
                            (expensive) timer animations run before doing anything with the new data.
                            Since the user will, in most cases (unless they quickly change tabs
                            immediately after stopping the timer), will be at the Timer screen. This
                            delay will not be noticeable, and will improve the feeling of responsiveness
                            at the Timer page.
                         */
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({ reloadList() }, 600)
                        }

                    ACTION_TIMES_MOVED_TO_HISTORY, ACTION_TIMES_MODIFIED -> reloadList()
                    ACTION_HISTORY_TIMES_SHOWN -> {
                        history = true
                        reloadList()
                    }

                    ACTION_SESSION_TIMES_SHOWN -> {
                        history = false
                        reloadList()
                    }
                }
            }
        }

    // Receives broadcasts about UI interactions that require actions to be taken.
    private val mUIInteractionReceiver
            : TTFragmentBroadcastReceiver =
        object : TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
            override fun onReceiveWhileAdded(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_DELETE_SELECTED_TIMES ->
                        // Operation will delete times and then broadcast "ACTION_TIMES_MODIFIED".
                        viewModel.deleteSolves(solveListAdapter!!.getSelectedIds())

                    ACTION_SCRAMBLE_MODIFIED ->
                        // A new scramble was generated
                        currentScramble = getScramble(intent)

                    ACTION_SELECTION_MODE_OFF ->
                        solveListAdapter?.clearSelection()

                    else -> {}
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=$savedInstanceState)")
        super.onCreate(savedInstanceState)
        mContext = context
        if (arguments != null) {
            currentPuzzle = requireArguments().getString(PUZZLE)
            currentPuzzleCategory = requireArguments().getString(PUZZLE_SUBTYPE)
            history = requireArguments().getBoolean(HISTORY)
            mode = requireArguments().getInt(MODE)
        }
        if (savedInstanceState != null) {
            currentScramble = savedInstanceState.getString("scramble")
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=$savedInstanceState)")
        binding = FragmentTimeListBinding.inflate(inflater, container, false)

        if (getBoolean(R.string.pk_show_clear_button, false)) {
            binding!!.clearButton.visibility = View.VISIBLE
        }

        binding!!.addTimeButton.setOnClickListener(clickListener)

        binding!!.archiveButton.setOnClickListener(clickListener)

        binding!!.clearButton.setOnClickListener(clickListener)

        binding!!.moreButton.setOnClickListener(clickListener)

        binding!!.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                searchComment = s.toString()
                reloadList()
            }
        })

        updateEasterEggs(binding!!.getRoot())

        setupRecyclerView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.solves.collect { solves ->
                    solveListAdapter?.submitList(solves)
                    setEmptyState(solves)
                }
            }
        }
        viewModel.updateParams(
            currentPuzzle ?: "",
            currentPuzzleCategory ?: "",
            mode,
            history,
            searchComment,
            orderByKey,
            orderByDir
        )

        registerReceiver(mTimeDataChangedReceiver)
        registerReceiver(mUIInteractionReceiver)

        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now and start observing further updates.
        onStatisticsUpdated(StatisticsCache.instance.statistics)
        StatisticsCache.instance.registerObserver(this) // Unregistered in "onDestroyView".

        return binding!!.getRoot()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("scramble", currentScramble)
    }

    override fun onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
        StatisticsCache.instance.unregisterObserver(this)
        mRecentStatistics = null
    }

    /**
     * Note: FAB is no longer used. This method does nothing.
     * 
     * @return
     * `true` if the "Back" button press was consumed to close the sheet; or
     * `false` if the sheet is not showing and the "Back" button press was ignored.
     */
    override fun onBackPressedInFragment(): Boolean {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()")
        return false
    }

    override fun onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()")
        super.onDetach()
        // To fix memory leaks
        unregisterReceiver(mTimeDataChangedReceiver)
        unregisterReceiver(mUIInteractionReceiver)
    }

    /**
     * Records the latest statistics for use when sharing such information.
     * 
     * @param stats The updated statistics. These will not be modified. May be `null`.
     */
    override fun onStatisticsUpdated(stats: Statistics?) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated($stats)")
        mRecentStatistics = stats
    }

    fun reloadList() {
        if (isAdded && !isDetached) {
            viewModel.updateParams(
                currentPuzzle ?: "",
                currentPuzzleCategory ?: "",
                mode,
                history,
                searchComment,
                orderByKey,
                orderByDir
            )
        }
    }


    fun setEmptyState(solves: List<Solve>) {
        if (solves.isEmpty()) {
            binding!!.warnEmptyList.visibility = View.VISIBLE
            binding!!.nothingText.visibility = View.VISIBLE
            if (history) {
                binding!!.nothingText.setText(R.string.list_empty_state_message_history)
            } else {
                binding!!.nothingText.setText(R.string.list_empty_state_message)
            }
        } else {
            binding!!.warnEmptyList.visibility = View.INVISIBLE
            binding!!.nothingText.visibility = View.INVISIBLE
        }
    }

    private fun setupRecyclerView() {
        val parentActivity: Activity = requireActivity()

        solveListAdapter = SolveListAdapter(requireActivity(), getParentFragmentManager())

        // Set different managers to support different orientations
        val gridLayoutManagerHorizontal =
            StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
        val gridLayoutManagerVertical =
            StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)

        // Adapt to orientation
        if (parentActivity.resources
                .configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) binding!!.list.setLayoutManager(gridLayoutManagerVertical)
        else binding!!.list.setLayoutManager(gridLayoutManagerHorizontal)

        binding!!.list.setAdapter(solveListAdapter)
    }

    private fun updateEasterEggs(root: RelativeLayout) {
        val date = DateTime(DateTime.now())

        // April 1st (April Fools)
        if (date.monthOfYear == 4 && date.dayOfMonth == 1) {
            val clippyView =
                LayoutInflater.from(mContext).inflate(
                    R.layout.item_easteregg_clippy,
                    requireView().parent as ViewGroup,
                    false
                )

            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )

            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)

            layoutParams.rightMargin = ThemeUtils.dpToPix(mContext!!, 8f)
            layoutParams.bottomMargin = ThemeUtils.dpToPix(mContext!!, 8f)

            val clippyLines = arrayOf<String?>(
                "It looks like you're having some trouble on that last layer.\n\nWould you like me to throw your cube away?",
                "Wow, is that a 10 by 10??",
                "It looks like you're relying too much on magnets\n\nWould you like to swap your cube for a Rubiks™ brand?",
                "Hey there, I'm Clippy, your new cubing assistant!",
                "I have a friend that can solve it in like, 3 seconds",
                "I mean, it's not that hard. Just some algorithms, right?",
                "It looks like you're trying to become color neutral.\n\nDid I mention I'm colorblind?",
                "To be honest, I just peel the stickers off",
                "I was walking through a cemetery once and saw a ghost cube.\n\nScary stuff.",
                "It looks like you're abusing that table too much\n\nWould you like to learn a real one-handed method?",
                "It looks like you're having some trouble with that 4x4 parity\n\nWould you like me to just peel off the stickers?",
                "I am Clippy. I am ethereal, ETERNAL-Oh, hey!\n\nWould you like some help?",
                "I once got six N perms in a row.\n\nI still have nightmares about that.",
                "I once solved like five sides, couldn't quite figure out the last one",
                "I CAN'T BELIEVE YOU GOT THAT PB, JUST AS MY SD CARD RUNS OUT",
                "I once used my Pyraminx as a fork. True story.",
                "Do you stop the timer with your feet too? Yuck.",
                "In the absence of a rock, a Megaminx makes a great substitute.\n\nOr so I've heard.",
                "It is the year 2049. You have been cubing non-stop for over 30 years now.\n\nDon't you think it's time to take a break?"
            )

            val clippyText = clippyView.findViewById<TextView>(R.id.clippy_text)
            clippyText.text = clippyLines[Random().nextInt(clippyLines.size)]

            clippyView.setOnClickListener { _: View? ->
                clippyView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { clippyView.visibility = View.GONE }
            }

            root.addView(clippyView, layoutParams)
        }
    }

    companion object {
        /**
         * Flag to enable debug logging for this class.
         */
        private const val DEBUG_ME = false

        /**
         * A "tag" to identify this class in log messages.
         */
        private val TAG: String = TimerListFragment::class.java.simpleName

        private const val PUZZLE = "puzzle"
        private const val PUZZLE_SUBTYPE = "puzzle_type"
        private const val HISTORY = "history"
        private const val MODE = "mode"

        // We have to put a boolean history here because it resets when we change puzzles.
        fun newInstance(
            puzzle: String?,
            puzzleType: String?,
            mode: Int,
            history: Boolean
        ): TimerListFragment {
            val fragment = TimerListFragment()
            val args = Bundle()
            args.putString(PUZZLE, puzzle)
            args.putBoolean(HISTORY, history)
            args.putString(PUZZLE_SUBTYPE, puzzleType)
            args.putInt(MODE, mode)
            fragment.setArguments(args)
            if (DEBUG_ME) Log.d(TAG, "newInstance() -> $fragment")
            return fragment
        }
    }
}
