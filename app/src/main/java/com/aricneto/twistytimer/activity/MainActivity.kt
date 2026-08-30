package com.aricneto.twistytimer.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.ActivityMainBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.TimerFragment;
import com.aricneto.twistytimer.fragment.TimerFragmentMain;
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog;
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog;
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain;
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.ExportImportUtils;
import com.aricneto.twistytimer.utils.LocaleUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mikepenz.materialdrawer.holder.ColorHolder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_COMMENT;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_DATE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_PENALTY;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_SCRAMBLE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_SUBTYPE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_TIME;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_TYPE;
import static com.aricneto.twistytimer.database.DatabaseHandler.ProgressListener;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

public class MainActivity extends AppCompatActivity
        implements ExportImportDialog.ExportImportCallbacks,
        PuzzleChooserDialog.PuzzleCallback {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    private static final int DEBUG_ID         = 11;
    private static final int TIMER_ID         = 1;
    private static final int THEME_ID         = 2;
    private static final int SCHEME_ID        = 9;
    private static final int OLL_ID           = 6;
    private static final int PLL_ID           = 7;
    private static final int EXPORT_IMPORT_ID = 10;
    private static final int ABOUT_ID         = 4;
    private static final int SETTINGS_ID      = 5;
    private static final int TRAINER_OLL_ID      = 14;
    private static final int TRAINER_PLL_ID      = 15;


    private static final int REQUEST_SETTING           = 42;
    private static final int REQUEST_ABOUT             = 23;

    private static final int EXPORT_BACKUP      = 50;
    private static final int EXPORT_EXTERNAL    = 51;
    private static final int IMPORT_BACKUP      = 60;
    private static final int IMPORT_EXTERNAL    = 61;

    /**
     * The fragment tag identifying the export/import dialog fragment.
     */
    private static final String FRAG_TAG_EXIM_DIALOG = "export_import_dialog";

    // NOTE: Loader IDs used by fragments need to be unique within the context of an activity that
    // creates those fragments. Therefore, it is safer to define all of the IDs in the same place.

    /**
     * The loader ID for the loader that loads data presented in the statistics table on the timer
     * graph fragment and the summary statistics on the timer fragment.
     */
    public static final int STATISTICS_LOADER_ID = 101;

    /**
     * The loader ID for the loader that loads chart data presented in on the timer graph fragment.
     */
    public static final int CHART_DATA_LOADER_ID = 102;

    /**
     * The loader ID for the loader that loads the list of solve times for the timer list fragment.
     */
    public static final int TIME_LIST_LOADER_ID = 103;

    /**
     * The loader ID for the loader that loads the list of algorithms for the algorithm list
     * fragment.
     */
    public static final int ALG_LIST_LOADER_ID = 104;

    SmoothActionBarDrawerToggle mDrawerToggle;
    FragmentManager             fragmentManager;
    DrawerLayout                mDrawerLayout;

    private MaterialDrawerSliderView mDrawer;

    private String mExportPuzzleType = "";
    private String mExportPuzzleCategory = "";

    /**
     * Sets drawer lock mode
     * {@code DrawerLayout.LOCK_MODE_LOCKED_CLOSED} for force closed and
     * {@code DrawerLayout.LOCK_MODE_LOCKED_UNDEFINED} for default behaviour
     */
    public void setDrawerLock(int lockMode) {
        mDrawerLayout.setDrawerLockMode(lockMode);
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(mDrawer);
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mDrawer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState="
                + savedInstanceState + "): " + this);

        setTheme(ThemeUtils.getPreferredTheme());

        // Set text styling
        if (!Prefs.getString(R.string.pk_text_style, "default").equals("default")) {
            getTheme().applyStyle(ThemeUtils.getPreferredTextStyle(), true);
        }

        // Set navigation bar tint
        if (Prefs.getBoolean(R.string.pk_tint_navigation_bar, false)) {
            getTheme().applyStyle(R.style.TintedNavigationBar, true);
            // Set navigation bar icon tint
            if (ThemeUtils.fetchAttrBool(this, ThemeUtils.getPreferredTheme(), R.styleable.BaseTwistyTheme_isLightTheme)) {
                getTheme().applyStyle(R.style.LightNavBarIconStyle, true);
            }
        }

        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentLayout, (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_container, TimerFragmentMain.newInstance(PuzzleUtils.TYPE_333, "Normal", TimerFragment.TIMER_MODE_TIMER, TrainerScrambler.TrainerSubset.OLL), "fragment_main")
                .commit();
        }

        handleDrawer(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(mDrawer)) {
                    mDrawerLayout.closeDrawer(mDrawer);
                    return;
                }

                final Fragment mainFragment = fragmentManager.findFragmentByTag("fragment_main");

                if (mainFragment instanceof OnBackPressedInFragmentListener) {
                    if (((OnBackPressedInFragmentListener) mainFragment).onBackPressedInFragment()) {
                        return;
                    }
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    @Override
    protected void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume(): " + this);
        try {
            super.onResume();
        } catch (ClassCastException e) {
            Log.e(TAG, "get life cycle exception (MIUI bug)", e);
        }
    }

    @Override
    protected void onPause() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onPause(): " + this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStart(): " + this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStop(): " + this);
        super.onStop();
    }

    private void handleDrawer(Bundle savedInstanceState) {
        mDrawer = findViewById(R.id.slider);
        mDrawerLayout = findViewById(R.id.root);

        ImageView headerView = (ImageView) View.inflate(this, R.layout.drawer_header, null);

        mDrawer.setHeaderView(headerView);

        headerView.setColorFilter(ThemeUtils.fetchAttrColor(this, androidx.appcompat.R.attr.colorPrimary), PorterDuff.Mode.MULTIPLY);

        int textColor = ThemeUtils.fetchAttrColor(this, R.attr.colorItemListText);
        int accentColor = ThemeUtils.fetchAttrColor(this, R.attr.colorItemListTextAction);
        int backgroundColor = ThemeUtils.fetchAttrColor(this, R.attr.colorBackgroundList);

        mDrawer.setBackgroundColor(backgroundColor);

        ColorStateList itemColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_selected},
                        new int[]{}
                },
                new int[]{
                        accentColor,
                        textColor
                }
        );

        ColorStateList normalColorStateList = ColorStateList.valueOf(textColor);

        headerView.setBackgroundColor(backgroundColor);

        PrimaryDrawerItem timerItem = new PrimaryDrawerItem();
        timerItem.setName(new StringHolder(R.string.drawer_title_timer));
        timerItem.setIcon(new ImageHolder(R.drawable.ic_outline_timer_24px));
        timerItem.setIdentifier((long) TIMER_ID);
        timerItem.setTextColor(itemColorStateList);
        timerItem.setIconColor(itemColorStateList);

        SecondaryDrawerItem trainerOllItem = new SecondaryDrawerItem();
        trainerOllItem.setName(new StringHolder(R.string.drawer_title_oll));
        trainerOllItem.setIcon(new ImageHolder(R.drawable.ic_oll_black_24dp));
        trainerOllItem.setIdentifier((long) TRAINER_OLL_ID);
        trainerOllItem.setTextColor(itemColorStateList);
        trainerOllItem.setIconColor(itemColorStateList);

        SecondaryDrawerItem trainerPllItem = new SecondaryDrawerItem();
        trainerPllItem.setName(new StringHolder(R.string.drawer_title_pll));
        trainerPllItem.setIcon(new ImageHolder(R.drawable.ic_pll_black_24dp));
        trainerPllItem.setIdentifier((long) TRAINER_PLL_ID);
        trainerPllItem.setTextColor(itemColorStateList);
        trainerPllItem.setIconColor(itemColorStateList);

        ExpandableDrawerItem trainerItem = new ExpandableDrawerItem();
        trainerItem.setName(new StringHolder(R.string.drawer_title_trainer));
        trainerItem.setIcon(new ImageHolder(R.drawable.ic_outline_control_camera_24px));
        trainerItem.setSelectable(false);
        trainerItem.setSubItems(Arrays.asList(trainerOllItem, trainerPllItem));
        trainerItem.setTextColor(itemColorStateList);
        trainerItem.setIconColor(itemColorStateList);

        SecondaryDrawerItem algsOllItem = new SecondaryDrawerItem();
        algsOllItem.setName(new StringHolder(R.string.drawer_title_oll));
        algsOllItem.setIcon(new ImageHolder(R.drawable.ic_oll_black_24dp));
        algsOllItem.setIdentifier((long) OLL_ID);
        algsOllItem.setTextColor(itemColorStateList);
        algsOllItem.setIconColor(itemColorStateList);

        SecondaryDrawerItem algsPllItem = new SecondaryDrawerItem();
        algsPllItem.setName(new StringHolder(R.string.drawer_title_pll));
        algsPllItem.setIcon(new ImageHolder(R.drawable.ic_pll_black_24dp));
        algsPllItem.setIdentifier((long) PLL_ID);
        algsPllItem.setTextColor(itemColorStateList);
        algsPllItem.setIconColor(itemColorStateList);

        ExpandableDrawerItem algorithmsItem = new ExpandableDrawerItem();
        algorithmsItem.setName(new StringHolder(R.string.title_algorithms));
        algorithmsItem.setIcon(new ImageHolder(R.drawable.ic_outline_library_books_24px));
        algorithmsItem.setSelectable(false);
        algorithmsItem.setSubItems(Arrays.asList(algsOllItem, algsPllItem));
        algorithmsItem.setTextColor(itemColorStateList);
        algorithmsItem.setIconColor(itemColorStateList);

        SectionDrawerItem otherSection = new SectionDrawerItem();
        otherSection.setName(new StringHolder(R.string.drawer_title_other));
        otherSection.setTextColor(normalColorStateList);

        PrimaryDrawerItem exportImportItem = new PrimaryDrawerItem();
        exportImportItem.setName(new StringHolder(R.string.drawer_title_export_import));
        exportImportItem.setIcon(new ImageHolder(R.drawable.ic_outline_folder_24px));
        exportImportItem.setSelectable(false);
        exportImportItem.setIdentifier((long) EXPORT_IMPORT_ID);
        exportImportItem.setTextColor(itemColorStateList);
        exportImportItem.setIconColor(itemColorStateList);

        PrimaryDrawerItem changeThemeItem = new PrimaryDrawerItem();
        changeThemeItem.setName(new StringHolder(R.string.drawer_title_changeTheme));
        changeThemeItem.setIcon(new ImageHolder(R.drawable.ic_outline_palette_24px));
        changeThemeItem.setSelectable(false);
        changeThemeItem.setIdentifier((long) THEME_ID);
        changeThemeItem.setTextColor(itemColorStateList);
        changeThemeItem.setIconColor(itemColorStateList);

        PrimaryDrawerItem changeColorSchemeItem = new PrimaryDrawerItem();
        changeColorSchemeItem.setName(new StringHolder(R.string.drawer_title_changeColorScheme));
        changeColorSchemeItem.setIcon(new ImageHolder(R.drawable.ic_outline_format_paint_24px));
        changeColorSchemeItem.setSelectable(false);
        changeColorSchemeItem.setIdentifier((long) SCHEME_ID);
        changeColorSchemeItem.setTextColor(itemColorStateList);
        changeColorSchemeItem.setIconColor(itemColorStateList);

        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem();
        settingsItem.setName(new StringHolder(R.string.action_settings));
        settingsItem.setIcon(new ImageHolder(R.drawable.ic_outline_settings_24px));
        settingsItem.setSelectable(false);
        settingsItem.setIdentifier((long) SETTINGS_ID);
        settingsItem.setTextColor(itemColorStateList);
        settingsItem.setIconColor(itemColorStateList);

        PrimaryDrawerItem aboutItem = new PrimaryDrawerItem();
        aboutItem.setName(new StringHolder(R.string.drawer_about));
        aboutItem.setIcon(new ImageHolder(R.drawable.ic_outline_help_outline_24px));
        aboutItem.setSelectable(false);
        aboutItem.setIdentifier((long) ABOUT_ID);
        aboutItem.setTextColor(itemColorStateList);
        aboutItem.setIconColor(itemColorStateList);

        mDrawer.getItemAdapter().add(
                timerItem,
                trainerItem,
                algorithmsItem,
                otherSection,
                exportImportItem,
                changeThemeItem,
                changeColorSchemeItem,
                new DividerDrawerItem(),
                settingsItem,
                aboutItem
        );

        if (BuildConfig.DEBUG) {
            SectionDrawerItem debugSection = new SectionDrawerItem();
            debugSection.setName(new StringHolder("DEBUG"));
            debugSection.setTextColor(normalColorStateList);

            PrimaryDrawerItem debugItem = new PrimaryDrawerItem();
            debugItem.setName(new StringHolder("DEBUG OPTION - ADD 10000 SOLVES"));
            debugItem.setIcon(new ImageHolder(R.drawable.ic_outline_help_outline_24px));
            debugItem.setSelectable(false);
            debugItem.setIdentifier((long) DEBUG_ID);
            debugItem.setTextColor(itemColorStateList);
            debugItem.setIconColor(itemColorStateList);

            mDrawer.getItemAdapter().add(
                    debugSection,
                    debugItem
            );
        }

        mDrawer.setOnDrawerItemClickListener(new Function3<View, IDrawerItem<?>, Integer, Boolean>() {
            @Override
            public Boolean invoke(View view, IDrawerItem<?> drawerItem, Integer integer) {

                boolean closeDrawer = true;

                switch ((int) drawerItem.getIdentifier()) {
                    default:
                        closeDrawer = false;
                    case TIMER_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                fragmentManager
                                        .beginTransaction()
                                        .replace(R.id.main_activity_container,
                                                 TimerFragmentMain.newInstance(PuzzleUtils.TYPE_333, "Normal", TimerFragment.TIMER_MODE_TIMER, TrainerScrambler.TrainerSubset.PLL), "fragment_main")
                                        .commit();
                            }
                        });
                        break;

                    case TRAINER_OLL_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                fragmentManager
                                        .beginTransaction()
                                        .replace(R.id.main_activity_container,
                                                 TimerFragmentMain.newInstance(TrainerScrambler.TrainerSubset.OLL.name(), "Normal", TimerFragment.TIMER_MODE_TRAINER, TrainerScrambler.TrainerSubset.OLL), "fragment_main")
                                        .commit();
                            }
                        });
                        break;

                    case TRAINER_PLL_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                fragmentManager
                                        .beginTransaction()
                                        .replace(R.id.main_activity_container,
                                                 TimerFragmentMain.newInstance(TrainerScrambler.TrainerSubset.PLL.name(), "Normal", TimerFragment.TIMER_MODE_TRAINER, TrainerScrambler.TrainerSubset.PLL), "fragment_main")
                                        .commit();
                            }
                        });
                        break;

                    case OLL_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                fragmentManager
                                        .beginTransaction()
                                        .replace(R.id.main_activity_container,
                                                 AlgListFragment.newInstance(DatabaseHandler.SUBSET_OLL),
                                                 "fragment_algs_oll")
                                        .commit();
                            }
                        });
                        break;

                    case PLL_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                fragmentManager
                                        .beginTransaction()
                                        .replace(R.id.main_activity_container,
                                                 AlgListFragment.newInstance(DatabaseHandler.SUBSET_PLL),
                                                 "fragment_algs_pll")
                                        .commit();
                            }
                        });
                        break;

                    case EXPORT_IMPORT_ID:
                        ExportImportDialog.newInstance()
                                .show(fragmentManager, FRAG_TAG_EXIM_DIALOG);
                        break;

                    case THEME_ID:
                        ThemeSelectDialog.newInstance().show(fragmentManager, "theme_dialog");
                        break;

                    case SCHEME_ID:
                        SchemeSelectDialogMain.newInstance()
                                .show(fragmentManager, "scheme_dialog");
                        break;

                    case SETTINGS_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                startActivityForResult(new Intent(
                                                               getApplicationContext(), SettingsActivity.class),
                                                       REQUEST_SETTING);
                            }
                        });
                        break;

                    case ABOUT_ID:
                        mDrawerToggle.runWhenIdle(new Runnable() {
                            @Override
                            public void run() {
                                startActivityForResult(new Intent(getApplicationContext(),
                                                                  AboutActivity.class), REQUEST_ABOUT);
                            }
                        });
                        break;

                    case DEBUG_ID:
                        if (BuildConfig.DEBUG) {
                            Random rand = new Random();
                            DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
                            for (int i = 0; i < 10000; i++) {
                                dbHandler.addSolve(new Solve(30000 + rand.nextInt(6000), "333",
                                                             "|<<# DEBUG #>>|", 165165L +(i*10), "", 0, "", rand.nextBoolean()));
                            }
                        }
                        break;
                }
                if (closeDrawer)
                    mDrawerLayout.closeDrawer(mDrawer);
                return false;
            }
        });

        /*if (savedInstanceState != null) {
            mDrawer.onRestoreInstanceState(savedInstanceState);
        }*/

        mDrawerToggle = new SmoothActionBarDrawerToggle(
                this, mDrawerLayout, null, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG_ME) Log.d(TAG, "onActivityResult(requestCode=" + requestCode
                + ", resultCode=" + resultCode + ", data=" + data + "): " + this);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTING) {
            if (DEBUG_ME) {
                Log.d(TAG, "  Returned from 'Settings'. Will recreate activity.");
            }
            onRecreateRequired();
        } else if ((requestCode == EXPORT_BACKUP || requestCode == EXPORT_EXTERNAL)
                && resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                Uri uri = data.getData();
                Log.d(TAG, "EXPORT : " + uri.toString());
                Log.d(TAG, "EXPORT : " + mExportPuzzleType + "," + mExportPuzzleCategory);

                new ExportSolves(this,
                        (requestCode == EXPORT_BACKUP ? ExportImportDialog.EXIM_FORMAT_BACKUP
                                : ExportImportDialog.EXIM_FORMAT_EXTERNAL),
                        uri, mExportPuzzleType, mExportPuzzleCategory)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else if ((requestCode == IMPORT_BACKUP || requestCode == IMPORT_EXTERNAL)
                && resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                Uri uri = data.getData();
                Log.d(TAG, "IMPORT : " + uri.toString());
                Log.d(TAG, "IMPORT : " + mExportPuzzleType + "," + mExportPuzzleCategory);

                new ImportSolves(this,
                        (requestCode == IMPORT_BACKUP ? ExportImportDialog.EXIM_FORMAT_BACKUP
                                : ExportImportDialog.EXIM_FORMAT_EXTERNAL),
                        uri, mExportPuzzleType, mExportPuzzleCategory)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    /**
     * Handles the need to recreate this activity due to a major change affecting the activity and
     * its fragments. For example, if the theme is changed by {@link ThemeSelectDialog}, or if
     * unknown changes have been made to the preferences in {@link SettingsActivity}.
     */
    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);

        // IMPORTANT: If this is not posted to the message queue, i.e., if "recreate()" is simply
        // called directly from "onRecreateRequired()" (or even if a flag is set here and
        // "recreate()" is called later from "onResume()", the recreation goes very wrong. After
        // the newly created activity instance calls "onResume()" it immediately calls "onPause()"
        // for no apparent reason whatsoever. The activity is clearly not "paused", as it the UI is
        // perfectly responsive. However, the next time it actually needs to pause, an exception is
        // logged complaining, "Performing pause of activity that is not resumed".
        //
        // Perhaps the issue is caused by an incorrect synchronisation of the destruction of the
        // old activity and the creation of the new activity. Whatever, simply posting the
        // "recreate()" call here seems to fix this. After posting, the (old) activity will
        // continue on and reach "onResume()" before then going through an orderly shutdown and
        // the new activity will be created and settle properly at "onResume()".
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                ActivityCompat.recreate(MainActivity.this);
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.updateLocale(newBase));
    }

    @Override
    protected void onDestroy() {
        if (DEBUG_ME) Log.d(TAG, "onDestroy(): " + this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overview, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState(): " + this);
        mDrawer.saveInstanceState(outState);
        //outState.putBoolean(OPEN_EXPORT_IMPORT_DIALOG, openExportImportDialog);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onImportSolveTimes(int fileFormat, String puzzleType, String puzzleCategory) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        mExportPuzzleType = puzzleType;
        mExportPuzzleCategory = puzzleCategory;

        if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
            startActivityForResult(intent, IMPORT_BACKUP);
        } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
            startActivityForResult(intent, IMPORT_EXTERNAL);
        }
    }

    @Override
    public void onExportSolveTimes(int fileFormat, String puzzleType, String puzzleCategory) {
        if (!StoreUtils.isExternalStorageWritable()) {
            return;
        }

        if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
            // Expect that all other parameters are null, otherwise something is very wrong.
            if (puzzleType != null || puzzleCategory != null) {
                throw new RuntimeException("Bug in the export code for the back-up format!");
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, ExportImportUtils.getBackupFileNameForExport());

            mExportPuzzleType = "";
            mExportPuzzleCategory = "";

            startActivityForResult(intent, EXPORT_BACKUP);
        } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
            // Expect that all other parameters are non-null, otherwise something is very wrong.
            if (puzzleType == null || puzzleCategory == null) {
                throw new RuntimeException("Bug in the export code for the external format!");
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE,
                    ExportImportUtils.getExternalFileNameForExport(puzzleType, puzzleCategory));

            mExportPuzzleType = puzzleType;
            mExportPuzzleCategory = puzzleCategory;

            startActivityForResult(intent, EXPORT_EXTERNAL);
        } else {
            Log.e(TAG, "Unknown export file format: " + fileFormat);
        }
    }

    /**
     * Handles the call-back from a fragment when a puzzle type and/or category are selected. This
     * is used for communication between the export/import fragments. The "source" fragment should
     * set the {@code tag} to the value of the fragment tag that this activity uses to identify
     * the "destination" fragment. This activity will then forward this notification to that
     * fragment, which is expected to implement this same interface method.
     */
    @Override
    public void onPuzzleSelected(
            @NonNull String tag, @NonNull String puzzleType, @NonNull String puzzleCategory) {
        // This "relay" scheme ensures that this activity is not embroiled in the gory details of
        // what the "destinationFrag" wanted with the puzzle type/category.
        final Fragment destinationFrag = fragmentManager.findFragmentByTag(tag);

        if (destinationFrag instanceof PuzzleChooserDialog.PuzzleCallback) {
            ((PuzzleChooserDialog.PuzzleCallback) destinationFrag)
                    .onPuzzleSelected(tag, puzzleType, puzzleCategory);
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onFileSelection(): Unknown or incompatible fragment: " + tag);
        }
    }

    private static class ExportSolves extends AsyncTask<Void, Integer, Boolean> {

        private final Activity  mContext;
        private final int      mFileFormat;
        private final Uri      mUri;
        private final String   mPuzzleType;
        private final String   mPuzzleCategory;

        private AlertDialog mProgressDialog;
        private ProgressBar mProgressBar;

        /**
         * Creates a new task for exporting solve times to a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param fileFormat
         *     The solve file format, must be {@link ExportImportDialog#EXIM_FORMAT_EXTERNAL}, or
         *     {@link ExportImportDialog#EXIM_FORMAT_BACKUP}.
         * @param uri
         *     The uri to which to export the solve times.
         * @param puzzleType
         *     The type of the puzzle whose times will be exported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be exported. Required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         */
        public ExportSolves(Activity context, int fileFormat, Uri uri,
                            String puzzleType, String puzzleCategory) {
            mContext = context;
            mFileFormat = fileFormat;
            mUri = uri;
            mPuzzleType = puzzleType;
            mPuzzleCategory = puzzleCategory;
        }

        @Override
        protected void onPreExecute() {
            View view = mContext.getLayoutInflater().inflate(R.layout.dialog_progress_m3, null);
            mProgressBar = view.findViewById(R.id.progress_bar);

            mProgressDialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.export_progress_title)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.action_done, null)
                .setNeutralButton(R.string.list_options_item_share, null)
                .create();

            mProgressDialog.show();
            mProgressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            mProgressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could legitimately be zero.
                    // Do not set max. to zero or it will display "NaN".
                    mProgressBar.setMax(Math.max(values[1], 1));
                }
                mProgressBar.setProgress(values[0]);
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Boolean returnCode;
            int exports = 0;

            try {
                final DatabaseHandler handler = TwistyTimer.getDBHandler();
                final OutputStream os = mContext.getContentResolver().openOutputStream(mUri);
                final OutputStreamWriter out = new OutputStreamWriter(os);

                if (mFileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                    String csvHeader
                            = "Puzzle,Category,Time(millis),Date(millis),Scramble,Penalty,Comment\n";
                    Cursor cursor = handler.getAllSolves();

                    try {
                        publishProgress(0, cursor.getCount());
                        out.write(csvHeader);

                        while (cursor.moveToNext()) {
                            out.write('"' + cursor.getString(IDX_TYPE)
                                    + "\";\"" + cursor.getString(IDX_SUBTYPE)
                                    + "\";\"" + cursor.getInt(IDX_TIME)
                                    + "\";\"" + cursor.getLong(IDX_DATE)
                                    + "\";\"" + cursor.getString(IDX_SCRAMBLE)
                                    + "\";\"" + cursor.getInt(IDX_PENALTY)
                                    + "\";\"" + cursor.getString(IDX_COMMENT)
                                    + "\"\n");
                            exports++;
                            publishProgress(exports);
                        }
                    } finally {
                        cursor.close();
                        out.close();
                    }
                    returnCode = true;
                } else if (mFileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                    Cursor cursor = handler.getAllSolvesFrom(mPuzzleType, mPuzzleCategory);

                    try {
                        publishProgress(0, cursor.getCount());

                        while (cursor.moveToNext()) {
                            String csvValues
                                    = '"' + PuzzleUtils.convertTimeToString(cursor.getInt(IDX_TIME), PuzzleUtils.FORMAT_DEFAULT)
                                    + "\";\"" + cursor.getString(IDX_SCRAMBLE)
                                    + "\";\"" + new DateTime(cursor.getLong(IDX_DATE))
                                    + '"';

                            // Add optional "DNF" in fourth field.
                            if (cursor.getInt(IDX_PENALTY) == PuzzleUtils.PENALTY_DNF) {
                                csvValues += ";\"DNF\"";
                            }

                            csvValues += '\n';

                            out.write(csvValues);
                            exports++;
                            publishProgress(exports);
                        }
                    } finally {
                        cursor.close();
                        out.close();
                    }
                    returnCode = true;
                } else {
                    Log.e(TAG, "Unknown export file format: " + mFileFormat);
                    returnCode = false;
                }
            } catch (IOException e) {
                returnCode = false;
                Log.d("ERROR", "IOException: " + e.getMessage());
            }

            return returnCode;
        }

        @Override
        protected void onPostExecute(Boolean isExported) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);

                if (isExported) {
                    mProgressDialog.setMessage(
                            Html.fromHtml(mContext.getString(R.string.export_progress_complete_wo_to)));
                    // Optional share action
                    mProgressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                    mProgressDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);

                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, mUri);
                        shareIntent.setType("application/octet-stream");

                        // FileProvider can sometimes crash devices lower than Lollipop
                        // due to permission issues, so we have to do some magic to the intent
                        // This is explained in a Medium post by @quiro91
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            shareIntent.setClipData(ClipData.newRawUri("", mUri));
                            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }

                        mContext.startActivity(Intent.createChooser(shareIntent, "Share"));
                        mProgressDialog.dismiss();
                    });
                } else {
                    mProgressDialog.setMessage(mContext.getString(R.string.export_progress_error));
                }
            }
        }
    }

    private static class ImportSolves extends AsyncTask<Void, Integer, Void> {

        private final Context  mContext;
        private final int      mFileFormat;
        private final Uri      mUri;
        private final String   mPuzzleType;
        private final String   mPuzzleCategory;

        private AlertDialog mProgressDialog;
        private ProgressBar mProgressBar;
        private int parseErrors = 0;
        private int duplicates  = 0;
        private int successes   = 0;

        /**
         * Creates a new task for importing solve times from a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param uri
         *     The file uri from which to import the solve times.
         * @param fileFormat
         *     The solve file format, must be {@link ExportImportDialog#EXIM_FORMAT_EXTERNAL}, or
         *     {@link ExportImportDialog#EXIM_FORMAT_BACKUP}.
         * @param puzzleType
         *     The type of the puzzle whose times will be imported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be imported. Required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         */
        public ImportSolves(Context context, int fileFormat, Uri uri,
                            String puzzleType, String puzzleCategory) {
            mContext = context;
            mFileFormat = fileFormat;
            mUri = uri;
            mPuzzleType = puzzleType;
            mPuzzleCategory = puzzleCategory;
        }

        @Override
        protected void onPreExecute() {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_progress_m3, null);
            mProgressBar = view.findViewById(R.id.progress_bar);

            mProgressDialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.import_progress_title)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.action_done, null)
                .create();
            mProgressDialog.show();
            mProgressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could legitimately be zero.
                    // Do not set max. to zero or it will display "NaN".
                    mProgressBar.setMax(Math.max(values[1], 1));
                }
                mProgressBar.setProgress(values[0]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Solve> solveList = new ArrayList<>();

            try {
                InputStream is = mContext.getContentResolver().openInputStream(mUri);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                CSVParser parser = new CSVParserBuilder()
                        .withSeparator(';')
                        .withQuoteChar('"')
                        .withStrictQuotes(true)
                        .build();
                CSVReader csvReader = new CSVReaderBuilder(br)
                        .withCSVParser(parser)
                        .build();
                String[] line;
                if (mFileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                    // throw away the header
                    csvReader.readNext();

                    while ((line = csvReader.readNext()) != null) {
                        try {
                            solveList.add(new Solve(
                                Integer.parseInt(line[2]), line[0], line[1], Long.parseLong(line[3]),
                                line[4], Integer.parseInt(line[5]), line[6], true));
                        } catch (Exception e) {
                            parseErrors++;
                        }
                    }
                } else if (mFileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                    final long now = DateTime.now().getMillis();

                    while ((line = csvReader.readNext()) != null) {
                        if (line.length <= 4) {
                            try {
                                Log.d("IMPORTING EXTERNAL", "time: " + line[0]);

                                int time = PuzzleUtils.parseTime(line[0]);
                                String scramble = "";
                                long date = now;
                                int penalty = PuzzleUtils.NO_PENALTY;

                                if (line.length >= 2) {
                                    scramble = line[1];
                                }
                                if (line.length >= 3) {
                                    try {
                                        date = DateTime.parse(line[2]).getMillis();
                                    } catch (Exception e) {
                                        // "date" remains equal to "now".
                                        e.printStackTrace();
                                    }
                                }
                                // Optional fourth field (index 3) may contain "DNF". If it is
                                // something else, ignore it.
                                if (line.length >= 4 && "DNF".equals(line[3])) {
                                    penalty = PuzzleUtils.PENALTY_DNF;
                                }

                                solveList.add(new Solve(
                                        time, mPuzzleType, mPuzzleCategory,
                                        date, scramble, penalty, "", true));
                            } catch (Exception e) {
                                parseErrors++;
                            }
                        } else {
                            parseErrors++;
                        }
                    }
                } else {
                    Log.e(TAG, "Unknown import file format: " + mFileFormat);
                }

                final DatabaseHandler handler = TwistyTimer.getDBHandler();

                // Perform a bulk insertion of the solves.
                successes = handler.addSolves(mFileFormat, solveList, new ProgressListener() {
                            @Override
                            public void onProgress(int numCompleted, int total) {
                                publishProgress(numCompleted, total);
                            }
                        });
                duplicates = solveList.size() - successes;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                mProgressDialog.setMessage(Html.fromHtml(
                        mContext.getString(R.string.import_progress_content)
                        + "<br><br><small><tt>"
                        + "<b>" + successes + "</b> "
                        + mContext.getString(R.string.import_progress_content_successful_imports)
                        + "<br><b>" + duplicates + "</b> "
                        + mContext.getString(R.string.import_progress_content_ignored_duplicates)
                        + "<br><b>" + parseErrors + "</b> "
                        + mContext.getString(R.string.import_progress_content_errors)
                        + "</small></tt>"));
            }
            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
        }
    }

    // So the drawer doesn't lag when closing
    private class SmoothActionBarDrawerToggle extends ActionBarDrawerToggle {

        private Runnable runnable;

        public SmoothActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            if (runnable != null && newState == DrawerLayout.STATE_IDLE) {
                runnable.run();
                runnable = null;
            }
        }

        public void runWhenIdle(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
