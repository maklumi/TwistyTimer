package com.aricneto.twistytimer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.util.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.FragmentTimerMainBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.fragment.dialog.BottomSheetTrainerDialog
import com.aricneto.twistytimer.fragment.dialog.CategorySelectDialog
import com.aricneto.twistytimer.fragment.dialog.PuzzleSelectDialog
import com.aricneto.twistytimer.fragment.dialog.PuzzleSelectDialog.Companion.newInstance
import com.aricneto.twistytimer.listener.DialogListenerMessage
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.stats.StatisticsCache
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.getPuzzleNameFromType
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY
import com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME
import com.aricneto.twistytimer.utils.TTIntent.ACTION_DELETE_SELECTED_TIMES
import com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE
import com.aricneto.twistytimer.utils.TTIntent.ACTION_HISTORY_TIMES_SHOWN
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SCROLLED_PAGE
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_OFF
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_ON
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SESSION_TIMES_SHOWN
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STARTED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STOPPED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_SELECTED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_UNSELECTED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TOOLBAR_RESTORED
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import com.aricneto.twistytimer.viewmodel.TimerViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

open class TimerFragmentMain : BaseFragment(), OnBackPressedInFragmentListener,
    DialogListenerMessage {
    private var binding: FragmentTimerMainBinding? = null

    private val viewModel: TimerViewModel by viewModels()

    var actionMode: ActionMode? = null

    private var tabStrip: LinearLayout? = null
    private var viewPagerAdapter: NavigationAdapter? = null

    // Stores the current puzzle being timed/shown
    private var currentPuzzle: String? = null
    private var currentPuzzleCategory: String? = null
    private var currentPuzzleSubset: TrainerSubset? = null
    private var currentTimerMode: String? = null

    // Stores the current state of the list switch
    var history: Boolean = false

    var currentPage: Int = TIMER_PAGE
    private var pagerEnabled = false

    private var selectCount = 0

    private var categoryDialog: CategorySelectDialog? = null

    private var currentModeInt: Int = 0

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.nav_button_category -> {
                categoryDialog = CategorySelectDialog.newInstance(
                    currentPuzzle,
                    currentPuzzleCategory,
                    currentTimerMode,
                    currentPuzzleSubset
                )
                categoryDialog?.setDialogListener(categoryDialogListener)
                categoryDialog?.show(parentFragmentManager, TAG_CATEGORY_DIALOG)
            }

            R.id.nav_button_history -> {
                history = !history

                updateHistorySwitchItem()

                broadcast(
                    CATEGORY_TIME_DATA_CHANGES,
                    if (history) ACTION_HISTORY_TIMES_SHOWN else ACTION_SESSION_TIMES_SHOWN
                )
            }

            R.id.nav_button_settings -> (activity as? MainActivity)?.openDrawer()
        }
    }

    private val categoryDialogListener: DialogListenerMessage = object : DialogListenerMessage {
        override fun onUpdateDialog(text: String?) {
            currentPuzzleCategory = text
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_CATEGORY)
        }
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.menu_list_callback, menu)

            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //getActivity().getWindow().setStatusBarColor(ThemeUtils.fetchAttrColor(mContext, R.attr.colorPrimaryDark));
            //}
            return true // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.delete -> {
                    // Receiver will delete times and then broadcast "ACTION_TIMES_MODIFIED".
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_DELETE_SELECTED_TIMES)
                    mode.finish()
                    return true
                }

                else -> return false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode?) {
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF)
        }
    }

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
        when (intent.action) {
            ACTION_CHANGED_THEME -> try {
                // If the theme has been changed, then the activity will need to be recreated. The
                // theme can only be applied properly during the inflation of the layouts, so it has
                // to go back to "Activity.updateLocale()" to do that.
                (activity as? MainActivity)?.onRecreateRequired()
            } catch (_: Exception) {
            }

            ACTION_TIMER_STARTED -> {
                (activity as? MainActivity)?.setDrawerLock(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                binding?.pager?.isUserInputEnabled = false
                activateTabLayout(false)
                binding?.actionbar?.toolbar?.animate()
                    ?.translationY(-(binding?.actionbar?.toolbar?.height?.toFloat() ?: 0f))
                    ?.alpha(0f)?.duration = mAnimationDuration.toLong()

                binding?.tabView?.animate()
                    ?.translationY(binding?.tabView?.height?.toFloat() ?: 0f)
                    ?.alpha(0f)?.duration = mAnimationDuration.toLong()
            }

            ACTION_TIMER_STOPPED -> {
                (activity as? MainActivity)?.setDrawerLock(DrawerLayout.LOCK_MODE_UNDEFINED)
                binding?.actionbar?.toolbar?.animate()
                    ?.translationY(0f)
                    ?.alpha(1f)?.duration = mAnimationDuration.toLong()

                binding?.tabView?.animate()
                    ?.translationY(0f)
                    ?.alpha(1f)
                    ?.setDuration(mAnimationDuration.toLong())
                    ?.withEndAction {
                        broadcast(
                            CATEGORY_UI_INTERACTIONS,
                            ACTION_TOOLBAR_RESTORED
                        )
                    }

                activateTabLayout(true)
                binding?.pager?.isUserInputEnabled = pagerEnabled
            }

            ACTION_SELECTION_MODE_ON -> {
                selectCount = 0
                actionMode =
                    binding?.actionbar?.toolbar?.startActionMode(actionModeCallback)
            }

            ACTION_SELECTION_MODE_OFF -> {
                selectCount = 0
                actionMode?.finish()
            }

            ACTION_TIME_SELECTED -> {
                selectCount += 1
                actionMode?.title =
                    resources.getQuantityString(
                        R.plurals.selected_list,
                        selectCount,
                        selectCount
                    )
            }

            ACTION_TIME_UNSELECTED -> {
                selectCount -= 1
                actionMode?.title =
                    resources.getQuantityString(
                        R.plurals.selected_list,
                        selectCount,
                        selectCount
                    )
            }

            ACTION_CHANGED_CATEGORY -> {
                binding?.pager?.adapter = viewPagerAdapter
                binding?.pager?.setCurrentItem(currentPage, false)
                updatePuzzleSpinnerHeader()
                viewModel.updateParams(
                    currentPuzzle,
                    currentPuzzleCategory,
                    currentModeInt,
                    history
                )

                if (currentTimerMode == TimerFragment.TIMER_MODE_TRAINER) broadcast(
                    CATEGORY_UI_INTERACTIONS,
                    ACTION_GENERATE_SCRAMBLE
                )
            }
        }
    }

    private var mContext: Context? = null
    private var mFragmentManager: FragmentManager? = null

    private var mAnimationDuration = 0

    @SuppressLint("StringFormatInvalid")
    private fun updatePuzzleSpinnerHeader() {
        history = false
        updateHistorySwitchItem()
        binding?.let { b ->
            b.actionbar.puzzleCategory.text =
                currentPuzzleCategory?.lowercase(Locale.getDefault()) ?: ""
            if (currentTimerMode == TimerFragment.TIMER_MODE_TRAINER) {
                b.actionbar.puzzleName.text = getString(
                    R.string.title_trainer, currentPuzzleSubset?.name ?: ""
                )
            } else {
                b.actionbar.puzzleName.setText(getPuzzleNameFromType(currentPuzzle))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState()")
        super.onSaveInstanceState(outState)
        outState.putString("puzzle", currentPuzzle)
        outState.putString("subtype", currentPuzzleCategory)
        outState.putSerializable("subset", currentPuzzleSubset)
        outState.putString("mode", currentTimerMode)
        outState.putBoolean("history", history)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=$savedInstanceState)")
        super.onCreate(savedInstanceState)

        mContext = context
        mFragmentManager = parentFragmentManager

        // Retrieve arguments
        if (arguments != null) {
            currentPuzzle = requireArguments().getString(PUZZLE)
            currentPuzzleCategory = requireArguments().getString(PUZZLE_SUBTYPE)
            currentTimerMode = requireArguments().getString(TIMER_MODE)
            currentModeInt = TimerFragment.modeToInt(currentTimerMode)
            currentPuzzleSubset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable(TRAINER_SUBSET, TrainerSubset::class.java)
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getSerializable(TRAINER_SUBSET) as TrainerSubset?
            }
        }

        // Retrieve instance state
        if (savedInstanceState != null) {
            currentPuzzle = savedInstanceState.getString("puzzle")
            currentPuzzleCategory = savedInstanceState.getString("subtype")
            currentTimerMode = savedInstanceState.getString("mode", TimerFragment.TIMER_MODE_TIMER)
            currentModeInt = TimerFragment.modeToInt(currentTimerMode)
            currentPuzzleSubset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("subset", TrainerSubset::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getSerializable("subset") as TrainerSubset?
            }
            history = savedInstanceState.getBoolean("history")

            // Set the dialog listeners again, in case the dialogs are open.
            categoryDialog = parentFragmentManager
                .findFragmentByTag(TAG_CATEGORY_DIALOG) as CategorySelectDialog?
            categoryDialog?.setDialogListener(categoryDialogListener)

            val selectDialog = parentFragmentManager
                .findFragmentByTag(TAG_PUZZLE_DIALOG) as PuzzleSelectDialog?
            selectDialog?.setDialogListener(this)
        }

        mAnimationDuration = getInt(
            R.string.pk_timer_animation_duration, resources.getInteger(
                R.integer.defaultAnimationDuration
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=$savedInstanceState)")
        binding = FragmentTimerMainBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return
        // setup background gradient
        binding.root.background =
            ThemeUtils.fetchBackgroundGradient(requireContext(), preferredTheme)

        binding.actionbar.navButtonCategory.setOnClickListener(clickListener)
        binding.actionbar.navButtonHistory.setOnClickListener(clickListener)
        binding.actionbar.navButtonSettings.setOnClickListener(clickListener)

        if (savedInstanceState == null) {
            // Remember last used puzzle
            currentPuzzle = Prefs.getString(R.string.pk_last_used_puzzle, PuzzleUtils.TYPE_333)
            updateCurrentCategory()
        }

        pagerEnabled = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
            getString(R.string.pk_tab_swiping_enabled), true
        )

        binding.pager.isUserInputEnabled = pagerEnabled

        // Menu bar background
        if (getBoolean(R.string.pk_menu_background, false)) {
            binding.actionbar.toolbar.setCardBackgroundColor(Color.TRANSPARENT)
            binding.actionbar.toolbar.cardElevation = 0f
        }

        viewPagerAdapter = NavigationAdapter(this)
        binding.pager.adapter = viewPagerAdapter
        binding.pager.offscreenPageLimit = NUM_PAGES - 1

        TabLayoutMediator(
            binding.mainTabs,
            binding.pager
        ) { tab: TabLayout.Tab?, position: Int ->
            when (position) {
                TIMER_PAGE -> tab?.setIcon(R.drawable.ic_outline_timer_24px)
                LIST_PAGE -> tab?.setIcon(R.drawable.ic_outline_list_alt_24px)
                GRAPH_PAGE -> tab?.setIcon(R.drawable.ic_outline_timeline_24px)
            }
        }.attach()

        binding.mainTabs.setSelectedTabIndicator(0)
        binding.mainTabs.tabIconTint =
        ResourcesCompat.getColorStateList(
            requireContext().resources,
            R.color.tab_color,
            requireContext().theme
        )

        tabStrip = (binding.mainTabs.getChildAt(0) as? LinearLayout)

        binding.mainTabs.background.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ThemeUtils.fetchAttrColor(requireContext(), R.attr.colorSurfaceContainer),
                BlendModeCompat.SRC_IN
            )

        // Handle spinner AFTER reading from savedInstanceState, so we can correctly
        // fill the category field in the spinner
        handleHeaderSpinner()

        binding.pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupPage(position)
                currentPage = position
            }

            override fun onPageScrollStateChanged(state: Int) {
                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SCROLLED_PAGE)
            }
        })

        viewModel.updateParams(currentPuzzle, currentPuzzleCategory, currentModeInt, history)
        observeStatistics()
        observeEvents()
    }

    private fun observeStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statistics.collectLatest { stats ->
                    StatisticsCache.instance.updateAndNotify(stats)
                }
            }
        }
    }

    private fun activateTabLayout(b: Boolean) {
        tabStrip?.isEnabled = b
        tabStrip?.let { strip ->
            for (i in 0 until strip.childCount) {
                strip.getChildAt(i).isClickable = b
            }
        }
    }

    override fun onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume() : currentPage=$currentPage")
        // Sets up the toolbar with the icons appropriate to the current page.
        binding!!.actionbar.toolbar.post { setupPage(currentPage) }
        super.onResume()
    }

    /**
     * Passes on the "Back" button press event to subordinate fragments and indicates if any
     * fragment consumed the event.
     * 
     * @return `true` if the "Back" button press was consumed and no further action should be
     * taken; or `false` if the "Back" button press was ignored and the caller should
     * propagate it to the next interested party.
     */
    override fun onBackPressedInFragment(): Boolean {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()")

        return viewPagerAdapter != null && viewPagerAdapter!!.dispatchOnBackPressedInFragment()
    }

    override fun onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()")
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun updateHistorySwitchItem() {
        if (history) {
            binding!!.actionbar.navButtonHistory.setImageResource(R.drawable.ic_history_on)
            binding!!.actionbar.navButtonHistory.animate()
                .rotation(-135f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(300)
                .start()
        } else {
            binding!!.actionbar.navButtonHistory.setImageResource(R.drawable.ic_history_off)
            binding!!.actionbar.navButtonHistory.animate()
                .rotation(0f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(300)
                .start()
        }
    }

    /**
     * Sets the page's toolbar buttons and other things
     * 
     * @param pageNum
     */
    private fun setupPage(pageNum: Int) {
        if (DEBUG_ME) Log.d(TAG, "setupPage(pageNum=$pageNum)")

        actionMode?.finish()

        val binding = binding ?: return

        when (pageNum) {
            TIMER_PAGE -> {
                binding.actionbar.navButtonHistory.animate()
                    .withStartAction {
                        binding.actionbar.navButtonHistory.isEnabled = false
                    }
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        // navButtonHistory may already be destroyed by the time we get
                        // to the end action, so we have to check if it still exists
                        this.binding?.actionbar?.navButtonHistory?.visibility = View.GONE
                    }
                    .start()
            }

            LIST_PAGE, GRAPH_PAGE -> {
                binding.actionbar.navButtonHistory.visibility = View.VISIBLE
                binding.actionbar.navButtonHistory.animate()
                    .withStartAction { binding.actionbar.navButtonHistory.isEnabled = true }
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    /**
     * The app saves the last subtype used for each puzzle. This function is called to both update
     * the last subtype when it's changed, and to set the subtype.
     */
    private fun updateCurrentCategory() {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
        val editor = sharedPreferences.edit()
        lifecycleScope.launch {
            val subtypeList = TwistyTimer.getSolveRepository()
                .getAllSubtypesFromType(currentPuzzle ?: "", currentModeInt)
            if (subtypeList.isEmpty()) {
                currentPuzzleCategory = "Normal"
                editor.putString(
                    getString(R.string.pk_last_used_category) + currentPuzzle + currentModeInt,
                    "Normal"
                )
                editor.apply()
            } else {
                currentPuzzleCategory = sharedPreferences.getString(
                    getString(R.string.pk_last_used_category) + currentPuzzle + currentModeInt,
                    "Normal"
                )
            }
            updatePuzzleSpinnerHeader()
        }
    }

    private fun handleHeaderSpinner() {
        // Setup action bar click listener
        binding?.actionbar?.puzzleSpinner?.setOnClickListener {
            if (currentTimerMode == TimerFragment.TIMER_MODE_TRAINER) {
                val bottomSheetTrainerDialog =
                    BottomSheetTrainerDialog.newInstance(currentPuzzleSubset, currentPuzzleCategory)
                bottomSheetTrainerDialog.show(parentFragmentManager, "trainer_dialog_fragment")
            } else {
                // Setup spinner dialog and adapter
                val puzzleSelectDialog = newInstance()
                puzzleSelectDialog.setDialogListener(this)
                puzzleSelectDialog.show(parentFragmentManager, TAG_PUZZLE_DIALOG)
            }
        }

        updatePuzzleSpinnerHeader()
    }

    override fun onUpdateDialog(text: String?) {
        currentPuzzle = text
        edit().putString(R.string.pk_last_used_puzzle, currentPuzzle).apply()
        updateCurrentCategory()
        binding?.pager?.adapter = viewPagerAdapter
        binding?.pager?.setCurrentItem(currentPage, false)

        val selectDialog =
            parentFragmentManager.findFragmentByTag(TAG_PUZZLE_DIALOG) as? PuzzleSelectDialog
        selectDialog?.dismiss()

        /** update titles **/
        updatePuzzleSpinnerHeader()
        viewModel.updateParams(currentPuzzle, currentPuzzleCategory, currentModeInt, history)
    }

    protected inner class NavigationAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val fragments = SparseArray<Fragment?>()

        override fun createFragment(position: Int): Fragment {
            if (DEBUG_ME) Log.d(TAG, "NavigationAdapter.createItem($position)")

            val fragment: Fragment
            when (position) {
                TIMER_PAGE -> fragment = TimerFragment.newInstance(
                    currentPuzzle, currentPuzzleCategory, currentTimerMode, currentPuzzleSubset
                )

                LIST_PAGE -> fragment = TimerListFragment.newInstance(
                    currentPuzzle, currentPuzzleCategory, currentModeInt, history
                )

                GRAPH_PAGE -> fragment = TimerGraphFragment.newInstance(
                    currentPuzzle, currentPuzzleCategory, currentModeInt, history
                )

                else -> fragment = TimerFragment.newInstance(
                    PuzzleUtils.TYPE_333,
                    "Normal",
                    TimerFragment.TIMER_MODE_TIMER,
                    TrainerSubset.OLL
                )
            }
            fragments.put(position, fragment)
            return fragment
        }

        /**
         * Notifies each fragment (that is listening) that the "Back" button has been pressed.
         * Stops when the first fragment consumes the event.
         * 
         * @return `true` if any fragment consumed the "Back" button press event; or `false`
         * if the event was not consumed by any fragment.
         */
        fun dispatchOnBackPressedInFragment(): Boolean {
            if (DEBUG_ME) Log.d(TAG, "NavigationAdapter.dispatchOnBackPressedInFragment()")
            var isConsumed = false

            for (i in 0 until fragments.size) {
                val fragment = fragments.valueAt(i)
                if (fragment is OnBackPressedInFragmentListener && fragment.isAdded) {
                    if (fragment.onBackPressedInFragment()) {
                        isConsumed = true
                        break
                    }
                }
            }

            return isConsumed
        }

        override fun getItemCount(): Int {
            return NUM_PAGES
        }
    }

    companion object {
        /**
         * Flag to enable debug logging for this class.
         */
        private const val DEBUG_ME = true

        /**
         * A "tag" to identify this class in log messages.
         */
        private val TAG: String = TimerFragmentMain::class.java.simpleName

        /**
         * The zero-based position of the timer fragment/tab/page.
         */
        const val TIMER_PAGE: Int = 0

        /**
         * The zero-based position of the timer list fragment/tab/page.
         */
        const val LIST_PAGE: Int = 1

        /**
         * The zero-based position of the timer graph fragment/tab/page.
         */
        const val GRAPH_PAGE: Int = 2

        /**
         * The total number of pages.
         */
        private const val NUM_PAGES = 3

        private const val PUZZLE = "puzzle"
        private const val PUZZLE_SUBTYPE = "puzzle_type"
        private const val TIMER_MODE = "timer_mode"
        private const val TRAINER_SUBSET = "trainer_subset"

        private const val TAG_CATEGORY_DIALOG = "select_category_dialog"
        private const val TAG_PUZZLE_DIALOG = "puzzle_spinner_dialog_fragment"

        @JvmStatic
        fun newInstance(
            puzzle: String?,
            category: String?,
            mode: String?,
            subset: TrainerSubset?
        ): TimerFragmentMain {
            val fragment = TimerFragmentMain()
            val args = Bundle()
            args.putString(PUZZLE, puzzle)
            args.putString(PUZZLE_SUBTYPE, category)
            args.putString(TIMER_MODE, mode)
            args.putSerializable(TRAINER_SUBSET, subset)
            fragment.setArguments(args)
            if (DEBUG_ME) Log.d(TAG, "newInstance() -> $fragment")
            return fragment
        }
    }
}
