package com.aricneto.twistytimer.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.aricneto.twistify.BuildConfig
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ActivityMainBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.AlgRepository
import com.aricneto.twistytimer.fragment.AlgListFragment.Companion.newInstance
import com.aricneto.twistytimer.fragment.TimerFragment
import com.aricneto.twistytimer.fragment.TimerFragmentMain.Companion.newInstance
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog.ExportImportCallbacks
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog.PuzzleCallback
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.utils.ExportImportUtils.backupFileNameForExport
import com.aricneto.twistytimer.utils.ExportImportUtils.getExternalFileNameForExport
import com.aricneto.twistytimer.utils.LocaleUtils.updateLocale
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.StoreUtils.isExternalStorageWritable
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTextStyle
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import com.aricneto.twistytimer.viewmodel.MainViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.max

class MainActivity : AppCompatActivity(), ExportImportCallbacks, PuzzleCallback {
    private var binding: ActivityMainBinding? = null

    private val viewModel: MainViewModel by viewModels()

    var mDrawerToggle: SmoothActionBarDrawerToggle? = null
    var fragmentManager: FragmentManager? = null
    var mDrawerLayout: DrawerLayout? = null

    private var mDrawer: MaterialDrawerSliderView? = null

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (DEBUG_ME) Log.d(TAG, "Returned from 'Settings'. Will recreate activity.")
            onRecreateRequired()
        }

    private val aboutLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // No action needed
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val params = viewModel.exportParams.value
                    importSolves(
                        ExportImportDialog.EXIM_FORMAT_BACKUP,
                        uri,
                        params.puzzleType,
                        params.puzzleCategory
                    )
                }
            }
        }

    private val importExternalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val params = viewModel.exportParams.value
                    importSolves(
                        ExportImportDialog.EXIM_FORMAT_EXTERNAL,
                        uri,
                        params.puzzleType,
                        params.puzzleCategory
                    )
                }
            }
        }

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val params = viewModel.exportParams.value
                    exportSolves(
                        ExportImportDialog.EXIM_FORMAT_BACKUP,
                        uri,
                        params.puzzleType,
                        params.puzzleCategory
                    )
                }
            }
        }

    private val exportExternalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val params = viewModel.exportParams.value
                    exportSolves(
                        ExportImportDialog.EXIM_FORMAT_EXTERNAL,
                        uri,
                        params.puzzleType,
                        params.puzzleCategory
                    )
                }
            }
        }

    /**
     * Sets drawer lock mode
     * `DrawerLayout.LOCK_MODE_LOCKED_CLOSED` for force closed and
     * `DrawerLayout.LOCK_MODE_LOCKED_UNDEFINED` for default behavior
     */
    fun setDrawerLock(lockMode: Int) {
        mDrawerLayout!!.setDrawerLockMode(lockMode)
    }

    fun openDrawer() {
        mDrawerLayout!!.openDrawer(mDrawer!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(
            TAG, ("updateLocale(savedInstanceState="
                    + savedInstanceState + "): " + this)
        )

        setTheme(preferredTheme)

        // Set text styling
        if (Prefs.getString(R.string.pk_text_style, "default") != "default") {
            theme.applyStyle(preferredTextStyle, true)
        }

        // Set navigation bar tint
        if (getBoolean(R.string.pk_tint_navigation_bar, false)) {
            theme.applyStyle(R.style.TintedNavigationBar, true)
            // Set navigation bar icon tint
            if (ThemeUtils.fetchAttrBool(this, android.R.attr.isLightTheme)) {
                theme.applyStyle(R.style.LightNavBarIconStyle, true)
            }
        }

        this.enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.contentLayout
        ) { v: View, insets: WindowInsetsCompat ->
            v.setPadding(
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        fragmentManager = supportFragmentManager

        if (savedInstanceState == null) {
            fragmentManager!!
                .beginTransaction()
                .replace(
                    R.id.main_activity_container,
                    newInstance(
                        PuzzleUtils.TYPE_333,
                        "Normal",
                        TimerFragment.TIMER_MODE_TIMER,
                        TrainerScrambler.TrainerSubset.OLL
                    ),
                    "fragment_main"
                )
                .commit()
        }

        handleDrawer()

        checkUpdateDialog()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mDrawerLayout!!.isDrawerOpen(mDrawer!!)) {
                    mDrawerLayout!!.closeDrawer(mDrawer!!)
                    return
                }

                val mainFragment: Fragment? = fragmentManager!!.findFragmentByTag("fragment_main")

                if (mainFragment is OnBackPressedInFragmentListener) {
                    if ((mainFragment as OnBackPressedInFragmentListener).onBackPressedInFragment()) {
                        return
                    }
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    override fun onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume(): $this")
        try {
            super.onResume()
        } catch (e: ClassCastException) {
            Log.e(TAG, "get life cycle exception", e)
        }
    }

    override fun onPause() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onPause(): $this")
        super.onPause()
    }

    override fun onStart() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStart(): $this")
        super.onStart()
    }

    override fun onStop() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStop(): $this")
        super.onStop()
    }

    private fun handleDrawer() {
        mDrawer = findViewById(R.id.slider)
        mDrawerLayout = findViewById(R.id.root)

        val headerView = View.inflate(this, R.layout.view_drawer_header, null) as ImageView
        mDrawer!!.headerView = headerView

        val textColor = MaterialColors.getColor(this, R.attr.colorOnSurface, Color.WHITE)
        val accentColor = MaterialColors.getColor(this, R.attr.colorTertiary, Color.BLUE)
        val backgroundColor = MaterialColors.getColor(this, R.attr.colorSurface, Color.BLACK)

        mDrawer!!.setBackgroundColor(backgroundColor)
        headerView.setBackgroundColor(backgroundColor)

        val itemColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(accentColor, accentColor, accentColor, textColor)
        )

        setupDrawerItems(itemColorStateList, accentColor, ColorStateList.valueOf(textColor))

        mDrawer!!.onDrawerItemClickListener = { _: View?, drawerItem: IDrawerItem<*>, _: Int? ->
            onDrawerItemClicked(drawerItem)
        }

        mDrawerToggle = SmoothActionBarDrawerToggle(
            this, mDrawerLayout!!, null, R.string.drawer_open, R.string.drawer_close
        )
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)
    }

    private fun setupDrawerItems(
        itemColorStateList: ColorStateList,
        accentColor: Int,
        normalColorStateList: ColorStateList
    ) {
        val timerItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_timer)
            icon = ImageHolder(R.drawable.ic_outline_timer_24px)
            identifier = TIMER_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        val trainerOllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_oll)
            icon = ImageHolder(R.drawable.ic_oll_black_24dp)
            identifier = TRAINER_OLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val trainerPllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_pll)
            icon = ImageHolder(R.drawable.ic_pll_black_24dp)
            identifier = TRAINER_PLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val trainerCmllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_cmll)
            icon = ImageHolder(R.drawable.ic_oll_black_24dp)
            identifier = TRAINER_CMLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val trainerItem = ExpandableDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_trainer)
            icon = ImageHolder(R.drawable.ic_outline_control_camera_24px)
            isSelectable = false
            subItems = mutableListOf(trainerOllItem, trainerPllItem, trainerCmllItem)
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            arrowColor = ColorHolder.fromColor(accentColor)
        }

        val algsOllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_oll)
            icon = ImageHolder(R.drawable.ic_oll_black_24dp)
            identifier = OLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val algsPllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_pll)
            icon = ImageHolder(R.drawable.ic_pll_black_24dp)
            identifier = PLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val algsCmllItem = SecondaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_cmll)
            icon = ImageHolder(R.drawable.ic_oll_black_24dp)
            identifier = CMLL_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            level = 2
        }

        val algorithmsItem = ExpandableDrawerItem().apply {
            name = StringHolder(R.string.title_algorithms)
            icon = ImageHolder(R.drawable.ic_outline_library_books_24px)
            isSelectable = false
            subItems = mutableListOf(algsOllItem, algsPllItem, algsCmllItem)
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
            arrowColor = ColorHolder.fromColor(accentColor)
        }

        val otherSection = SectionDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_other)
            textColor = normalColorStateList
        }

        val exportImportItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_export_import)
            icon = ImageHolder(R.drawable.ic_outline_folder_24px)
            isSelectable = false
            identifier = EXPORT_IMPORT_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        val changeThemeItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_changeTheme)
            icon = ImageHolder(R.drawable.ic_outline_palette_24px)
            isSelectable = false
            identifier = THEME_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        val changeColorSchemeItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_title_changeColorScheme)
            icon = ImageHolder(R.drawable.ic_outline_format_paint_24px)
            isSelectable = false
            identifier = SCHEME_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        val settingsItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.action_settings)
            icon = ImageHolder(R.drawable.ic_outline_settings_24px)
            isSelectable = false
            identifier = SETTINGS_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        val aboutItem = PrimaryDrawerItem().apply {
            name = StringHolder(R.string.drawer_about)
            icon = ImageHolder(R.drawable.ic_outline_help_outline_24px)
            isSelectable = false
            identifier = ABOUT_ID.toLong()
            textColor = itemColorStateList
            iconColor = itemColorStateList
            isIconTinted = true
        }

        mDrawer!!.itemAdapter.add(
            timerItem,
            trainerItem,
            algorithmsItem,
            otherSection,
            exportImportItem,
            changeThemeItem,
            changeColorSchemeItem,
            DividerDrawerItem(),
            settingsItem,
            aboutItem
        )

        if (BuildConfig.DEBUG) {
            val debugSection = SectionDrawerItem().apply {
                name = StringHolder("DEBUG")
                textColor = normalColorStateList
            }

            val debugItem = PrimaryDrawerItem().apply {
                name = StringHolder("DEBUG OPTION - ADD 10000 SOLVES")
                icon = ImageHolder(R.drawable.ic_outline_help_outline_24px)
                isSelectable = false
                identifier = DEBUG_ID.toLong()
                textColor = itemColorStateList
                iconColor = itemColorStateList
                isIconTinted = true
            }

            mDrawer!!.itemAdapter.add(debugSection, debugItem)
        }
    }

    private fun onDrawerItemClicked(drawerItem: IDrawerItem<*>): Boolean {
        var closeDrawer = true
        when (drawerItem.identifier.toInt()) {
            TIMER_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(
                            PuzzleUtils.TYPE_333,
                            "Normal",
                            TimerFragment.TIMER_MODE_TIMER,
                            TrainerScrambler.TrainerSubset.PLL
                        ), "fragment_main"
                    )
                    .commit()
            }

            TRAINER_OLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(
                            TrainerScrambler.TrainerSubset.OLL.name,
                            "Normal",
                            TimerFragment.TIMER_MODE_TRAINER,
                            TrainerScrambler.TrainerSubset.OLL
                        ), "fragment_main"
                    )
                    .commit()
            }

            TRAINER_PLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(
                            TrainerScrambler.TrainerSubset.PLL.name,
                            "Normal",
                            TimerFragment.TIMER_MODE_TRAINER,
                            TrainerScrambler.TrainerSubset.PLL
                        ), "fragment_main"
                    )
                    .commit()
            }

            TRAINER_CMLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(
                            TrainerScrambler.TrainerSubset.CMLL.name,
                            "Normal",
                            TimerFragment.TIMER_MODE_TRAINER,
                            TrainerScrambler.TrainerSubset.CMLL
                        ), "fragment_main"
                    )
                    .commit()
            }

            OLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(AlgRepository.SUBSET_OLL),
                        "fragment_algs_oll"
                    )
                    .commit()
            }

            PLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(AlgRepository.SUBSET_PLL),
                        "fragment_algs_pll"
                    )
                    .commit()
            }

            CMLL_ID -> mDrawerToggle!!.runWhenIdle {
                fragmentManager!!
                    .beginTransaction()
                    .replace(
                        R.id.main_activity_container,
                        newInstance(AlgRepository.SUBSET_CMLL),
                        "fragment_algs_cmll"
                    )
                    .commit()
            }

            EXPORT_IMPORT_ID -> ExportImportDialog.newInstance()
                .show(fragmentManager!!, FRAG_TAG_EXIM_DIALOG)

            THEME_ID -> ThemeSelectDialog.newInstance()
                .show(fragmentManager!!, "theme_dialog")

            SCHEME_ID -> SchemeSelectDialogMain.newInstance()
                .show(fragmentManager!!, "scheme_dialog")

            SETTINGS_ID -> mDrawerToggle!!.runWhenIdle {
                settingsLauncher.launch(Intent(applicationContext, SettingsActivity::class.java))
            }

            ABOUT_ID -> mDrawerToggle!!.runWhenIdle {
                aboutLauncher.launch(Intent(applicationContext, AboutActivity::class.java))
            }

            DEBUG_ID -> if (BuildConfig.DEBUG) {
                val rand = Random()
                val solveRepository = TwistyTimer.getSolveRepository()
                lifecycleScope.launch {
                    var i = 0
                    while (i < 10000) {
                        solveRepository.insertSolve(
                            type = "333",
                            subtype = "|<<# DEBUG #>>|",
                            time = (30000 + rand.nextInt(6000)).toLong(),
                            date = 165165L + (i * 10),
                            scramble = "",
                            penalty = 0,
                            comment = "",
                            history = rand.nextBoolean(),
                            mode = 0
                        )
                        i++
                    }
                }
            }

            else -> {
                closeDrawer = false
                mDrawerToggle!!.runWhenIdle {
                    fragmentManager!!
                        .beginTransaction()
                        .replace(
                            R.id.main_activity_container,
                            newInstance(
                                PuzzleUtils.TYPE_333,
                                "Normal",
                                TimerFragment.TIMER_MODE_TIMER,
                                TrainerScrambler.TrainerSubset.PLL
                            ), "fragment_main"
                        )
                        .commit()
                }
            }
        }
        if (closeDrawer) mDrawerLayout!!.closeDrawer(mDrawer!!)
        return false
    }

    private fun checkUpdateDialog() {
        if (getBoolean(R.string.pk_show_update_dialog, false)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setIcon(R.drawable.icon_launch)
                .setMessage(R.string.pref_summary_new)
                .setPositiveButton(R.string.action_done) { _, _ ->
                    Prefs.edit {
                        putBoolean(R.string.pk_show_update_dialog, false)
                    }
                }
                .show()
        }
    }


    /**
     * Handles the need to recreate this activity due to a major change affecting the activity and
     * its fragments. For example, if the theme is changed by [ThemeSelectDialog], or if
     * unknown changes have been made to the preferences in [SettingsActivity].
     */
    fun onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): $this")

        Handler(Looper.getMainLooper()).post {
            if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: $this")
            ActivityCompat.recreate(this@MainActivity)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    override fun onDestroy() {
        if (DEBUG_ME) Log.d(TAG, "onDestroy(): $this")
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_overview, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState(): $this")
        mDrawer!!.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onImportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?, mode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"

        viewModel.updateExportParams(puzzleType, puzzleCategory, mode)

        if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
            importBackupLauncher.launch(intent)
        } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
            importExternalLauncher.launch(intent)
        }
    }

    override fun onExportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?, mode: Int) {
        if (!isExternalStorageWritable()) {
            return
        }

        when (fileFormat) {
            ExportImportDialog.EXIM_FORMAT_BACKUP -> {
                // Expect that all other parameters are null, otherwise something is very wrong.
                if (puzzleType != null || puzzleCategory != null) {
                    throw RuntimeException("Bug in the export code for the back-up format!")
                }

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TITLE, backupFileNameForExport)

                viewModel.updateExportParams("", "", 0)

                exportBackupLauncher.launch(intent)
            }

            ExportImportDialog.EXIM_FORMAT_EXTERNAL -> {
                // Expect that all other parameters are non-null, otherwise something is very wrong.
                if (puzzleType == null || puzzleCategory == null) {
                    throw RuntimeException("Bug in the export code for the external format!")
                }

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/plain"
                intent.putExtra(
                    Intent.EXTRA_TITLE,
                    getExternalFileNameForExport(puzzleType, puzzleCategory)
                )

                viewModel.updateExportParams(puzzleType, puzzleCategory, mode)

                exportExternalLauncher.launch(intent)
            }

            else -> {
                Log.e(TAG, "Unknown export file format: $fileFormat")
            }
        }
    }

    /**
     * Handles the call-back from a fragment when a puzzle type and/or category are selected. This
     * is used for communication between the export/import fragments. The "source" fragment should
     * set the `tag` to the value of the fragment tag that this activity uses to identify
     * the "destination" fragment. This activity will then forward this notification to that
     * fragment, which is expected to implement this same interface method.
     */
    override fun onPuzzleSelected(
        tag: String, puzzleType: String, puzzleCategory: String
    ) {
        // This "relay" scheme ensures that this activity is not embroiled in the gory details of
        // what the "destinationFrag" wanted with the puzzle type/category.
        val destinationFrag: Fragment? = fragmentManager!!.findFragmentByTag(tag)

        if (destinationFrag is PuzzleCallback) {
            (destinationFrag as PuzzleCallback)
                .onPuzzleSelected(tag, puzzleType, puzzleCategory)
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onFileSelection(): Unknown or incompatible fragment: $tag")
        }
    }

    /**
     * Exports solve times to a file using Coroutines and ImportExportManager.
     */
    private fun exportSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String
    ) {
        val manager = TwistyTimer.getImportExportManager()
        val params = viewModel.exportParams.value

        lifecycleScope.launch {
            val view = layoutInflater.inflate(R.layout.dialog_progress_m3, null)
            val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

            val progressDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.export_progress_title)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.action_done, null)
                .setNeutralButton(R.string.list_options_item_share, null)
                .create()

            progressDialog.show()
            progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.GONE
            progressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.GONE

            val isExported = manager.exportSolves(
                fileFormat, uri, puzzleType, puzzleCategory, params.mode
            ) { count, total ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.max = max(total, 1)
                    progressBar.progress = count
                }
            }

            progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
            if (isExported) {
                progressDialog.setMessage(
                    Html.fromHtml(
                        getString(R.string.export_progress_complete_wo_to),
                        FROM_HTML_MODE_LEGACY
                    )
                )
                progressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.VISIBLE
                progressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "application/octet-stream"
                    startActivity(Intent.createChooser(shareIntent, "Share"))
                    progressDialog.dismiss()
                }
            } else {
                progressDialog.setMessage(getString(R.string.export_progress_error))
            }
        }
    }

    /**
     * Imports solve times from a file using Coroutines and ImportExportManager.
     */
    private fun importSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String
    ) {
        val manager = TwistyTimer.getImportExportManager()
        val params = viewModel.exportParams.value

        lifecycleScope.launch {
            val view = layoutInflater.inflate(R.layout.dialog_progress_m3, null)
            val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

            val progressDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.import_progress_title)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.action_done, null)
                .create()
            progressDialog.show()
            progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.GONE

            val result = manager.importSolves(
                fileFormat, uri, puzzleType, puzzleCategory, params.mode
            ) { count, total ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.max = max(total, 1)
                    progressBar.progress = count
                }
            }

            val (successes, duplicates, parseErrors) = result
            progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
            progressDialog.setMessage(
                Html.fromHtml(
                    (getString(R.string.import_progress_content)
                            + "<br><br><small><tt>"
                            + "<b>" + successes + "</b> "
                            + getString(R.string.import_progress_content_successful_imports)
                            + "<br><b>" + duplicates + "</b> "
                            + getString(R.string.import_progress_content_ignored_duplicates)
                            + "<br><b>" + parseErrors + "</b> "
                            + getString(R.string.import_progress_content_errors)
                            + "</small></tt>"),
                    FROM_HTML_MODE_LEGACY
                )
            )
            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
        }
    }

    // So the drawer doesn't lag when closing
    inner class SmoothActionBarDrawerToggle(
        activity: Activity?,
        drawerLayout: DrawerLayout?,
        toolbar: MaterialToolbar?,
        openDrawerContentDescRes: Int,
        closeDrawerContentDescRes: Int
    ) : ActionBarDrawerToggle(
        activity,
        drawerLayout,
        toolbar,
        openDrawerContentDescRes,
        closeDrawerContentDescRes
    ) {
        private var runnable: Runnable? = null

        override fun onDrawerStateChanged(newState: Int) {
            super.onDrawerStateChanged(newState)
            if (newState == DrawerLayout.STATE_IDLE) {
                runnable?.run()
                runnable = null
            }
        }

        fun runWhenIdle(runnable: Runnable?) {
            this.runnable = runnable
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
        private val TAG: String = MainActivity::class.java.simpleName

        private const val DEBUG_ID = 11
        private const val TIMER_ID = 1
        private const val THEME_ID = 2
        private const val SCHEME_ID = 9
        private const val OLL_ID = 6
        private const val PLL_ID = 7
        private const val CMLL_ID = 8
        private const val EXPORT_IMPORT_ID = 10
        private const val ABOUT_ID = 4
        private const val SETTINGS_ID = 5
        private const val TRAINER_OLL_ID = 14
        private const val TRAINER_PLL_ID = 15
        private const val TRAINER_CMLL_ID = 16


        /**
         * The fragment tag identifying the export/import dialog fragment.
         */
        private const val FRAG_TAG_EXIM_DIALOG = "export_import_dialog"


    }
}
