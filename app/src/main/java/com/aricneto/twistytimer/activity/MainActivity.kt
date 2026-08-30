package com.aricneto.twistytimer.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import com.aricneto.twistytimer.database.DatabaseHandler
import com.aricneto.twistytimer.fragment.AlgListFragment.Companion.newInstance
import com.aricneto.twistytimer.fragment.TimerFragment
import com.aricneto.twistytimer.fragment.TimerFragmentMain.Companion.newInstance
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog.ExportImportCallbacks
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog.PuzzleCallback
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.utils.ExportImportUtils.backupFileNameForExport
import com.aricneto.twistytimer.utils.ExportImportUtils.getExternalFileNameForExport
import com.aricneto.twistytimer.utils.LocaleUtils.updateLocale
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.aricneto.twistytimer.utils.StoreUtils.isExternalStorageWritable
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrBool
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTextStyle
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Random
import kotlin.math.max

class MainActivity : AppCompatActivity(), ExportImportCallbacks, PuzzleCallback {
    private var binding: ActivityMainBinding? = null

    var mDrawerToggle: SmoothActionBarDrawerToggle? = null
    var fragmentManager: FragmentManager? = null
    var mDrawerLayout: DrawerLayout? = null

    private var mDrawer: MaterialDrawerSliderView? = null

    private var mExportPuzzleType: String = PuzzleUtils.TYPE_333
    private var mExportPuzzleCategory: String = PuzzleUtils.TYPE_333

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
                    importSolves(
                        ExportImportDialog.EXIM_FORMAT_BACKUP,
                        uri,
                        mExportPuzzleType,
                        mExportPuzzleCategory
                    )
                }
            }
        }

    private val importExternalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    importSolves(
                        ExportImportDialog.EXIM_FORMAT_EXTERNAL,
                        uri,
                        mExportPuzzleType,
                        mExportPuzzleCategory
                    )
                }
            }
        }

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    exportSolves(
                        ExportImportDialog.EXIM_FORMAT_BACKUP,
                        uri,
                        mExportPuzzleType,
                        mExportPuzzleCategory
                    )
                }
            }
        }

    private val exportExternalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    exportSolves(
                        ExportImportDialog.EXIM_FORMAT_EXTERNAL,
                        uri,
                        mExportPuzzleType,
                        mExportPuzzleCategory
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
            if (fetchAttrBool(this, preferredTheme, R.styleable.BaseTwistyTheme_isLightTheme)) {
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

        val headerView = View.inflate(this, R.layout.drawer_header, null) as ImageView

        mDrawer!!.headerView = headerView

        headerView.setColorFilter(
            fetchAttrColor(this, androidx.appcompat.R.attr.colorPrimary),
            PorterDuff.Mode.MULTIPLY
        )

        val textColor = fetchAttrColor(this, R.attr.colorItemListText)
        val accentColor = fetchAttrColor(this, R.attr.colorItemListTextAction)
        val backgroundColor = fetchAttrColor(this, R.attr.colorBackgroundList)

        mDrawer!!.setBackgroundColor(backgroundColor)

        val itemColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(
                accentColor,
                textColor
            )
        )

        val normalColorStateList = ColorStateList.valueOf(textColor)

        headerView.setBackgroundColor(backgroundColor)

        val timerItem = PrimaryDrawerItem()
        timerItem.name = StringHolder(R.string.drawer_title_timer)
        timerItem.icon = ImageHolder(R.drawable.ic_outline_timer_24px)
        timerItem.identifier = TIMER_ID.toLong()
        timerItem.textColor = itemColorStateList
        timerItem.iconColor = itemColorStateList

        val trainerOllItem = SecondaryDrawerItem()
        trainerOllItem.name = StringHolder(R.string.drawer_title_oll)
        trainerOllItem.icon = ImageHolder(R.drawable.ic_oll_black_24dp)
        trainerOllItem.identifier = TRAINER_OLL_ID.toLong()
        trainerOllItem.textColor = itemColorStateList
        trainerOllItem.iconColor = itemColorStateList

        val trainerPllItem = SecondaryDrawerItem()
        trainerPllItem.name = StringHolder(R.string.drawer_title_pll)
        trainerPllItem.icon = ImageHolder(R.drawable.ic_pll_black_24dp)
        trainerPllItem.identifier = TRAINER_PLL_ID.toLong()
        trainerPllItem.textColor = itemColorStateList
        trainerPllItem.iconColor = itemColorStateList

        val trainerItem = ExpandableDrawerItem()
        trainerItem.name = StringHolder(R.string.drawer_title_trainer)
        trainerItem.icon = ImageHolder(R.drawable.ic_outline_control_camera_24px)
        trainerItem.isSelectable = false
        trainerItem.subItems = mutableListOf(trainerOllItem, trainerPllItem)
        trainerItem.textColor = itemColorStateList
        trainerItem.iconColor = itemColorStateList

        val algsOllItem = SecondaryDrawerItem()
        algsOllItem.name = StringHolder(R.string.drawer_title_oll)
        algsOllItem.icon = ImageHolder(R.drawable.ic_oll_black_24dp)
        algsOllItem.identifier = OLL_ID.toLong()
        algsOllItem.textColor = itemColorStateList
        algsOllItem.iconColor = itemColorStateList

        val algsPllItem = SecondaryDrawerItem()
        algsPllItem.name = StringHolder(R.string.drawer_title_pll)
        algsPllItem.icon = ImageHolder(R.drawable.ic_pll_black_24dp)
        algsPllItem.identifier = PLL_ID.toLong()
        algsPllItem.textColor = itemColorStateList
        algsPllItem.iconColor = itemColorStateList

        val algorithmsItem = ExpandableDrawerItem()
        algorithmsItem.name = StringHolder(R.string.title_algorithms)
        algorithmsItem.icon = ImageHolder(R.drawable.ic_outline_library_books_24px)
        algorithmsItem.isSelectable = false
        algorithmsItem.subItems = mutableListOf(algsOllItem, algsPllItem)
        algorithmsItem.textColor = itemColorStateList
        algorithmsItem.iconColor = itemColorStateList

        val otherSection = SectionDrawerItem()
        otherSection.name = StringHolder(R.string.drawer_title_other)
        otherSection.textColor = normalColorStateList

        val exportImportItem = PrimaryDrawerItem()
        exportImportItem.name = StringHolder(R.string.drawer_title_export_import)
        exportImportItem.icon = ImageHolder(R.drawable.ic_outline_folder_24px)
        exportImportItem.isSelectable = false
        exportImportItem.identifier = EXPORT_IMPORT_ID.toLong()
        exportImportItem.textColor = itemColorStateList
        exportImportItem.iconColor = itemColorStateList

        val changeThemeItem = PrimaryDrawerItem()
        changeThemeItem.name = StringHolder(R.string.drawer_title_changeTheme)
        changeThemeItem.icon = ImageHolder(R.drawable.ic_outline_palette_24px)
        changeThemeItem.isSelectable = false
        changeThemeItem.identifier = THEME_ID.toLong()
        changeThemeItem.textColor = itemColorStateList
        changeThemeItem.iconColor = itemColorStateList

        val changeColorSchemeItem = PrimaryDrawerItem()
        changeColorSchemeItem.name = StringHolder(R.string.drawer_title_changeColorScheme)
        changeColorSchemeItem.icon = ImageHolder(R.drawable.ic_outline_format_paint_24px)
        changeColorSchemeItem.isSelectable = false
        changeColorSchemeItem.identifier = SCHEME_ID.toLong()
        changeColorSchemeItem.textColor = itemColorStateList
        changeColorSchemeItem.iconColor = itemColorStateList

        val settingsItem = PrimaryDrawerItem()
        settingsItem.name = StringHolder(R.string.action_settings)
        settingsItem.icon = ImageHolder(R.drawable.ic_outline_settings_24px)
        settingsItem.isSelectable = false
        settingsItem.identifier = SETTINGS_ID.toLong()
        settingsItem.textColor = itemColorStateList
        settingsItem.iconColor = itemColorStateList

        val aboutItem = PrimaryDrawerItem()
        aboutItem.name = StringHolder(R.string.drawer_about)
        aboutItem.icon = ImageHolder(R.drawable.ic_outline_help_outline_24px)
        aboutItem.isSelectable = false
        aboutItem.identifier = ABOUT_ID.toLong()
        aboutItem.textColor = itemColorStateList
        aboutItem.iconColor = itemColorStateList

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
            val debugSection = SectionDrawerItem()
            debugSection.name = StringHolder("DEBUG")
            debugSection.textColor = normalColorStateList

            val debugItem = PrimaryDrawerItem()
            debugItem.name = StringHolder("DEBUG OPTION - ADD 10000 SOLVES")
            debugItem.icon = ImageHolder(R.drawable.ic_outline_help_outline_24px)
            debugItem.isSelectable = false
            debugItem.identifier = DEBUG_ID.toLong()
            debugItem.textColor = itemColorStateList
            debugItem.iconColor = itemColorStateList

            mDrawer!!.itemAdapter.add(
                debugSection,
                debugItem
            )
        }

        mDrawer!!.onDrawerItemClickListener =
            { _: View?, drawerItem: IDrawerItem<*>, _: Int? ->
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

                    OLL_ID -> mDrawerToggle!!.runWhenIdle {
                        fragmentManager!!
                            .beginTransaction()
                            .replace(
                                R.id.main_activity_container,
                                newInstance(DatabaseHandler.SUBSET_OLL),
                                "fragment_algs_oll"
                            )
                            .commit()
                    }

                    PLL_ID -> mDrawerToggle!!.runWhenIdle {
                        fragmentManager!!
                            .beginTransaction()
                            .replace(
                                R.id.main_activity_container,
                                newInstance(DatabaseHandler.SUBSET_PLL),
                                "fragment_algs_pll"
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
                        settingsLauncher.launch(
                            Intent(
                                applicationContext,
                                SettingsActivity::class.java
                            )
                        )
                    }

                    ABOUT_ID -> mDrawerToggle!!.runWhenIdle {
                        aboutLauncher.launch(Intent(applicationContext, AboutActivity::class.java))
                    }

                    DEBUG_ID -> if (BuildConfig.DEBUG) {
                        val rand = Random()
                        val dbHandler = TwistyTimer.getDBHandler()
                        var i = 0
                        while (i < 10000) {
                            dbHandler.addSolve(
                                Solve(
                                    30000 + rand.nextInt(6000),
                                    "333",
                                    "|<<# DEBUG #>>|",
                                    165165L + (i * 10),
                                    "",
                                    0,
                                    "",
                                    rand.nextBoolean()
                                )
                            )
                            i++
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
                false
            }

        mDrawerToggle = SmoothActionBarDrawerToggle(
            this, mDrawerLayout!!, null, R.string.drawer_open, R.string.drawer_close
        )
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)
    }


    /**
     * Handles the need to recreate this activity due to a major change affecting the activity and
     * its fragments. For example, if the theme is changed by [ThemeSelectDialog], or if
     * unknown changes have been made to the preferences in [SettingsActivity].
     */
    fun onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): $this")

        // IMPORTANT: If this is not posted to the message queue, i.e., if "recreate()" is simply
        // called directly from "onRecreateRequired()" (or even if a flag is set here and
        // "recreate()" is called later from "onResume()", the recreation goes very wrong. After
        // the newly created activity instance calls "onResume()" it immediately calls "onPause()"
        // for no apparent reason whatsoever. The activity is clearly not "paused", as it the UI is
        // perfectly responsive. However, the next time it actually needs to pause, an exception is
        // logged complaining, "Performing pause of activity that is not resumed".
        //
        // Perhaps the issue is caused by an incorrect synchronization of the destruction of the
        // old activity and the creation of the new activity. Whatever, simply posting the
        // "recreate()" call here seems to fix this. After posting, the (old) activity will
        // continue on and reach "onResume()" before then going through an orderly shutdown and
        // the new activity will be created and settle properly at "onResume()".
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: $this")
                ActivityCompat.recreate(this@MainActivity)
            }
        })
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

    override fun onImportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"

        mExportPuzzleType = puzzleType ?: ""
        mExportPuzzleCategory = puzzleCategory ?: ""

        if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
            importBackupLauncher.launch(intent)
        } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
            importExternalLauncher.launch(intent)
        }
    }

    override fun onExportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?) {
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

                mExportPuzzleType = ""
                mExportPuzzleCategory = ""

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

                mExportPuzzleType = puzzleType
                mExportPuzzleCategory = puzzleCategory

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
     * Exports solve times to a file using Coroutines.
     */
    private fun exportSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String
    ) {
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

            val isExported = withContext(Dispatchers.IO) {
                var returnCode: Boolean
                var exports = 0

                try {
                    val handler = TwistyTimer.getDBHandler()
                    val os = contentResolver.openOutputStream(uri)
                    val out = OutputStreamWriter(os)

                    when (fileFormat) {
                        ExportImportDialog.EXIM_FORMAT_BACKUP -> {
                            val csvHeader =
                                "Puzzle,Category,Time(millis),Date(millis),Scramble,Penalty,Comment\n"
                            val cursor = handler.allSolves

                            try {
                                withContext(Dispatchers.Main) {
                                    progressBar.max = max(cursor.count, 1)
                                    progressBar.progress = 0
                                }
                                out.write(csvHeader)

                                while (cursor.moveToNext()) {
                                    out.write(
                                        ("\"" + cursor.getString(DatabaseHandler.IDX_TYPE)
                                                + "\";\"" + cursor.getString(DatabaseHandler.IDX_SUBTYPE)
                                                + "\";\"" + cursor.getInt(DatabaseHandler.IDX_TIME)
                                                + "\";\"" + cursor.getLong(DatabaseHandler.IDX_DATE)
                                                + "\";\"" + cursor.getString(DatabaseHandler.IDX_SCRAMBLE)
                                                + "\";\"" + cursor.getInt(DatabaseHandler.IDX_PENALTY)
                                                + "\";\"" + cursor.getString(DatabaseHandler.IDX_COMMENT)
                                                + "\"\n")
                                    )
                                    exports++
                                    withContext(Dispatchers.Main) {
                                        progressBar.progress = exports
                                    }
                                }
                            } finally {
                                cursor.close()
                                out.close()
                            }
                            returnCode = true
                        }

                        ExportImportDialog.EXIM_FORMAT_EXTERNAL -> {
                            val cursor = handler.getAllSolvesFrom(puzzleType, puzzleCategory)

                            try {
                                withContext(Dispatchers.Main) {
                                    progressBar.max = max(cursor.count, 1)
                                    progressBar.progress = 0
                                }

                                while (cursor.moveToNext()) {
                                    var csvValues = ("\"" + convertTimeToString(
                                        cursor.getInt(DatabaseHandler.IDX_TIME).toLong(),
                                        PuzzleUtils.FORMAT_DEFAULT
                                    )
                                            + "\";\"" + cursor.getString(DatabaseHandler.IDX_SCRAMBLE)
                                            + "\";\"" + DateTime(cursor.getLong(DatabaseHandler.IDX_DATE))
                                            + "\"")

                                    if (cursor.getInt(DatabaseHandler.IDX_PENALTY) == PuzzleUtils.PENALTY_DNF) {
                                        csvValues += ";\"DNF\""
                                    }

                                    csvValues += '\n'

                                    out.write(csvValues)
                                    exports++
                                    withContext(Dispatchers.Main) {
                                        progressBar.progress = exports
                                    }
                                }
                            } finally {
                                cursor.close()
                                out.close()
                            }
                            returnCode = true
                        }

                        else -> {
                            Log.e(TAG, "Unknown export file format: $fileFormat")
                            returnCode = false
                        }
                    }
                } catch (e: IOException) {
                    returnCode = false
                    Log.d("ERROR", "IOException: " + e.message)
                }
                returnCode
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
     * Imports solve times from a file using Coroutines.
     */
    private fun importSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String
    ) {
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

            val result = withContext(Dispatchers.IO) {
                val solveList: MutableList<Solve> = ArrayList()
                var parseErrors = 0
                var successes = 0

                try {
                    val `is` = contentResolver.openInputStream(uri)
                        ?: throw IOException("Could not open input stream")
                    val isr = InputStreamReader(`is`)
                    val br = BufferedReader(isr)
                    val parser = CSVParserBuilder()
                        .withSeparator(';')
                        .withQuoteChar('"')
                        .withStrictQuotes(true)
                        .build()
                    val csvReader = CSVReaderBuilder(br)
                        .withCSVParser(parser)
                        .build()

                    if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                        csvReader.readNext() // throw away header

                        while (true) {
                            val nextLine = csvReader.readNext() ?: break
                            try {
                                if (nextLine.size >= 7) {
                                    solveList.add(
                                        Solve(
                                            nextLine[2]?.toIntOrNull()
                                                ?: throw Exception("Invalid time"),
                                            nextLine[0] ?: "",
                                            nextLine[1] ?: "",
                                            nextLine[3]?.toLongOrNull() ?: 0L,
                                            nextLine[4] ?: "",
                                            nextLine[5]?.toIntOrNull() ?: 0,
                                            nextLine[6] ?: "",
                                            true
                                        )
                                    )
                                } else {
                                    parseErrors++
                                }
                            } catch (_: Exception) {
                                parseErrors++
                            }
                        }
                    } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                        val now = DateTime.now().millis

                        while (true) {
                            val nextLine = csvReader.readNext() ?: break
                            if (nextLine.size <= 4) {
                                try {
                                    val time = PuzzleUtils.parseTime(
                                        nextLine[0] ?: throw Exception("Missing time")
                                    )
                                    var scramble = ""
                                    var date = now
                                    var penalty = PuzzleUtils.NO_PENALTY

                                    if (nextLine.size >= 2) scramble = nextLine[1] ?: ""
                                    if (nextLine.size >= 3) {
                                        nextLine[2]?.let {
                                            try {
                                                date = DateTime.parse(it).millis
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                    if (nextLine.size >= 4 && "DNF" == nextLine[3]) {
                                        penalty = PuzzleUtils.PENALTY_DNF
                                    }

                                    solveList.add(
                                        Solve(
                                            time, puzzleType, puzzleCategory,
                                            date, scramble, penalty, "", true
                                        )
                                    )
                                } catch (_: Exception) {
                                    parseErrors++
                                }
                            } else {
                                parseErrors++
                            }
                        }
                    }

                    val handler = TwistyTimer.getDBHandler()
                    successes = handler.addSolves(
                        fileFormat,
                        solveList,
                        object : DatabaseHandler.ProgressListener {
                            override fun onProgress(numCompleted: Int, total: Int) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    progressBar.max = max(total, 1)
                                    progressBar.progress = numCompleted
                                }
                            }
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Triple(successes, solveList.size - successes, parseErrors)
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
        toolbar: Toolbar?,
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
        private const val EXPORT_IMPORT_ID = 10
        private const val ABOUT_ID = 4
        private const val SETTINGS_ID = 5
        private const val TRAINER_OLL_ID = 14
        private const val TRAINER_PLL_ID = 15


        /**
         * The fragment tag identifying the export/import dialog fragment.
         */
        private const val FRAG_TAG_EXIM_DIALOG = "export_import_dialog"

        // NOTE: Loader IDs used by fragments need to be unique within the context of an activity that
        // creates those fragments. Therefore, it is safer to define all the IDs in the same place.
        /**
         * The loader ID for the loader that loads data presented in the statistics table on the timer
         * graph fragment and the summary statistics on the timer fragment.
         */
        const val STATISTICS_LOADER_ID: Int = 101

        /**
         * The loader ID for the loader that loads chart data presented in on the timer graph fragment.
         */
        const val CHART_DATA_LOADER_ID: Int = 102

    }
}
