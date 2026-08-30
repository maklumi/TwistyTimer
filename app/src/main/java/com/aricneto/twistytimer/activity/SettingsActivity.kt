package com.aricneto.twistytimer.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.ActivitySettingsBinding;
import com.aricneto.twistytimer.fragment.dialog.CrossHintFaceSelectDialog;
import com.aricneto.twistytimer.fragment.dialog.LocaleSelectDialog;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.utils.LocaleUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = SettingsActivity.class.getSimpleName();


    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=" + savedInstanceState + ")");

        setTheme(R.style.SettingsTheme);

        LocaleUtils.updateLocale(getApplicationContext());

        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            // Add the main "parent" settings fragment. It is not added to be back stack, so that
            // when "Back" is pressed, the "SettingsActivity" will exit, which is appropriate.
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new SettingsFragment(), "fragment_settings")
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                final Fragment settingsFragment = getSupportFragmentManager().findFragmentByTag("fragment_settings");

                if (settingsFragment instanceof OnBackPressedInFragmentListener) {
                    if (((OnBackPressedInFragmentListener) settingsFragment).onBackPressedInFragment()) {
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
        try {
            super.onResume();
        } catch (ClassCastException e) {
            Log.e(TAG, "get life cycle exception (MIUI bug)", e);
        }
    }

    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                ActivityCompat.recreate(SettingsActivity.this);
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.updateLocale(newBase));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnBackPressedInFragmentListener {

        // Variables used to handle back button behavior
        // Stores last PreferenceScreen opened
        private PreferenceScreen lastPreferenceScreen;
        // Stores the main PreferenceScreen
        private PreferenceScreen mainScreen;

        Context mContext;

        private int inspectionDuration;

        private String averageText;

        private final androidx.preference.Preference.OnPreferenceClickListener clickListener
                = new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_inspection_time,
                        R.string.pk_show_scramble_x_cross_hints,
                        R.string.pref_screen_title_timer_appearance_settings,
                        R.string.pref_screen_other_general,
                        R.string.pref_screen_other_list,
                        R.string.pk_locale,
                        R.string.pk_options_show_scramble_hints,
                        R.string.pk_timer_text_size,
                        R.string.pk_timer_text_offset,
                        R.string.pk_scramble_image_size,
                        R.string.pk_scramble_text_size,
                        R.string.pk_advanced_timer_settings_enabled,
                        R.string.pk_stat_trim_size,
                        R.string.pk_timer_animation_duration)) {

                    case R.string.pk_inspection_time:
                        createNumberDialog(R.string.inspection_time, R.string.pk_inspection_time);
                        break;

                    case R.string.pk_show_scramble_x_cross_hints:
                        if (Prefs.getBoolean(R.string.pk_show_scramble_x_cross_hints, false)) {
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.showHintsXCrossSummary)
                                    .setPositiveButton(R.string.action_ok, null)
                                    .show();
                        }
                        break;

                    case R.string.pk_options_show_scramble_hints:
                        CrossHintFaceSelectDialog.newInstance()
                                .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "cross_hint_face_dialog");
                        break;

                    case R.string.pk_locale:
                        if (getActivity() instanceof AppCompatActivity) {
                            LocaleSelectDialog.newInstance()
                                    .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "locale_dialog");
                        } else {
                            Log.e(TAG, "Could not find correct activity to launch dialog!");
                        }
                        break;

                    case R.string.pk_timer_text_size:
                        createSeekTextSizeDialog(R.string.pk_timer_text_size, 60, "12.34", true);
                        break;

                    case R.string.pk_scramble_image_size:
                        createImageSeekDialog(
                                R.string.pk_scramble_image_size, R.string.scrambleImageSize_text);
                        break;

                    case R.string.pk_scramble_text_size:
                        createSeekTextSizeDialog(R.string.pk_scramble_text_size,
                                14, "R U R' U' R' F R2 U' R' U' R U R' F'", false);
                        break;

                    case R.string.pk_advanced_timer_settings_enabled:
                        if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled, false)) {
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.advanced_pref_summary)
                                    .setPositiveButton(R.string.action_ok, null)
                                    .show();
                        }
                        break;
                    case R.string.pk_timer_animation_duration:
                        createSeekDialog(R.string.pk_timer_animation_duration,
                                         0, 1000,
                                         R.integer.defaultAnimationDuration,
                                         "%d ms");
                        break;
                    case R.string.pk_stat_trim_size:
                        // This would be a lot cleaner with high-order functions, but I couldn't find a way to get it working for API < 24
                        final View trimDialogView = LayoutInflater.from(
                                getActivity()).inflate(R.layout.dialog_settings_progress, null);
                        final AppCompatSeekBar trimSeekBar = trimDialogView.findViewById(R.id.seekbar);
                        final TextView trimText = trimDialogView.findViewById(R.id.text);

                        int defaultValue = getContext().getResources().getInteger(R.integer.defaultTrimSize);

                        trimSeekBar.setMax(30);
                        trimSeekBar.setProgress(Prefs.getInt(R.string.pk_stat_trim_size, defaultValue));

                        AlertDialog trimDialog = new MaterialAlertDialogBuilder(mContext)
                                .setView(trimDialogView)
                                .setPositiveButton(R.string.action_done, (dialog, which) -> {
                                    Prefs.edit()
                                            .putInt(R.string.pk_stat_trim_size, trimSeekBar.getProgress() > 0 ? trimSeekBar.getProgress() : 0)
                                            .apply();
                                })
                                .setNegativeButton(R.string.action_cancel, null)
                                .setNeutralButton(R.string.action_default, (dialog, which) -> {
                                    Prefs.edit()
                                            .putInt(R.string.pk_stat_trim_size, defaultValue)
                                            .apply();
                                })
                                .create();


                        SeekBar.OnSeekBarChangeListener trimChangeListener = new SeekBar.OnSeekBarChangeListener() {
                            int ao50, ao100, ao1000;
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                ao50 = getTrim(50, progress);
                                ao100 = getTrim(100, progress);
                                ao1000 = getTrim(1000, progress);
                                trimText.setText(String.format(
                                        getString(R.string.pref_dialog_trim_size, progress) + "%%\n\n" +
                                        getString(R.string.pref_dialog_included_solves) +
                                                               "\n\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d",
                                                               averageText + 3, 0,
                                                               averageText + 5, 2,
                                                               averageText + 12, 2,
                                                               averageText + 50, ao50,
                                                               averageText + 100, ao100,
                                                               averageText + 1000, ao1000));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        };

                        trimSeekBar.setOnSeekBarChangeListener(trimChangeListener);
                        trimChangeListener.onProgressChanged(trimSeekBar, trimSeekBar.getProgress(), false);
                        trimDialog.show();
                        break;
                }
                return false;
            }
        };


        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            super.onCreate(savedInstanceState);

            mContext = getContext();

            averageText = getString(R.string.graph_legend_avg_prefix);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);

            int listenerPrefIds[] = {R.string.pk_inspection_time,
                    R.string.pref_screen_title_timer_appearance_settings,
                    R.string.pref_screen_other_general,
                    R.string.pref_screen_other_list,
                    R.string.pk_show_scramble_x_cross_hints,
                    R.string.pk_locale,
                    R.string.pk_options_show_scramble_hints,
                    R.string.pk_timer_text_size,
                    R.string.pk_scramble_text_size,
                    R.string.pk_scramble_image_size,
                    R.string.pk_advanced_timer_settings_enabled,
                    R.string.pk_stat_trim_size,
                    R.string.pk_timer_animation_duration};

            for (int prefId : listenerPrefIds) {
                Preference preference = findPreference(getString(prefId));
                if (preference != null) {
                    preference.setOnPreferenceClickListener(clickListener);
                }
            }

            mainScreen = getPreferenceScreen();
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set the Inspection Alert preference summary to display the correct information
            // about time elapsed depending on user's current inspection duration
            updateInspectionAlertText();
        }

        private void updateInspectionAlertText() {
            inspectionDuration = Prefs.getInt(R.string.pk_inspection_time, 15);

            Preference inspectionPreference = findPreference(getString(R.string.pk_inspection_alert_enabled));
            if (inspectionPreference != null)
                    inspectionPreference.setSummary(getString(R.string.pref_inspection_alert_summary,
                            inspectionDuration == 15 ? 8 : (int) (inspectionDuration * 0.5f),
                            inspectionDuration == 15 ? 12 : (int) (inspectionDuration * 0.8f)));
        }

        @Override
        public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
            lastPreferenceScreen = getPreferenceScreen();
            setPreferenceScreen(preferenceScreen);
        }

        /**
         * PreferenceFragmentCompat does not handle back button behavior by default.
         * To implement the correct behavior, we store the last opened {@link PreferenceScreen} and
         * the main {@link PreferenceScreen}. When back button is pressed, we check if the current
         * screen is the same as mainScreen, if it is, we return false so the Activity can handle
         * it by closing {@link SettingsActivity}. If not, we set our screen to the last one we opened
         *
         * @return true if back button was consumed
         */
        @Override
        public boolean onBackPressedInFragment() {
            if (lastPreferenceScreen != null && getPreferenceScreen() != lastPreferenceScreen) {
                setPreferenceScreen(lastPreferenceScreen);
                return true;
            }
            return false;
        }

        private void createNumberDialog(@StringRes int title, final int prefKeyResID) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
            TextInputEditText editText = view.findViewById(R.id.edit_text);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setText(String.valueOf(Prefs.getInt(prefKeyResID, 15)));

            new MaterialAlertDialogBuilder(mContext)
                    .setTitle(title)
                    .setView(view)
                    .setPositiveButton(R.string.action_done, (dialog, which) -> {
                        try {
                            final int time = Integer.parseInt(editText.getText().toString());
                            Prefs.edit().putInt(prefKeyResID, time).apply();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getActivity(), R.string.invalid_time, Toast.LENGTH_SHORT).show();
                        }
                        updateInspectionAlertText();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setNeutralButton(R.string.action_default, (dialog, which) -> {
                        Prefs.edit().putInt(prefKeyResID, 15).apply();
                        updateInspectionAlertText();
                    })
                    .show();
        }

        private void createSeekDialog(@StringRes int prefKeyResID,
                                      int minValue, int maxValue, @IntegerRes int defaultValueRes,
                                      String formatText) {

            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar = dialogView.findViewById(R.id.seekbar);
            final TextView text = dialogView.findViewById(R.id.text);

            int defaultValue = getContext().getResources().getInteger(defaultValueRes);

            seekBar.setMax(maxValue);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, defaultValue));

            text.setText(String.format(formatText, seekBar.getProgress()));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    text.setText(String.format(formatText, seekBar.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            new MaterialAlertDialogBuilder(mContext)
                    .setView(dialogView)
                    .setPositiveButton(R.string.action_done, (dialog, which) -> {
                        final int seekProgress = seekBar.getProgress();

                        Prefs.edit()
                                .putInt(prefKeyResID, seekProgress > minValue ? seekProgress : minValue)
                                .apply();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setNeutralButton(R.string.action_default, (dialog, which) -> Prefs.edit().putInt(prefKeyResID, defaultValue).apply())
                    .show();
        }

        private int getTrim(int avg, int trim) {
            return ((int) Math.ceil(avg * (trim / 100f)) * 2);
        }

        private void createSeekTextSizeDialog(
                final int prefKeyResID, int defaultTextSize, String showText, boolean bold) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
            final float defaultTextSizePx = text.getTextSize();
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    defaultTextSizePx * (seekBar.getProgress() / 100f));
            if (bold)
                text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setText(showText);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (i / 100f));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new MaterialAlertDialogBuilder(mContext)
                    .setView(dialogView)
                    .setPositiveButton(R.string.action_done, (dialog, which) -> {
                        final int seekProgress = seekBar.getProgress();

                        Prefs.edit()
                                .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                .apply();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setNeutralButton(R.string.action_default, (dialog, which) -> {
                        Prefs.edit().putInt(prefKeyResID, 100).apply();
                    })
                    .show();
        }

        private void createImageSeekDialog(final int prefKeyResID, @StringRes int title) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress_image, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final View image = dialogView.findViewById(R.id.image);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            final int defaultWidth = image.getLayoutParams().width;
            final int defaultHeight = image.getLayoutParams().height;

            image.getLayoutParams().width *= (seekBar.getProgress() / 100f);
            image.getLayoutParams().height *= (seekBar.getProgress() / 100f);


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    LinearLayout.LayoutParams params
                            = (LinearLayout.LayoutParams) image.getLayoutParams();
                    params.width = (int) (defaultWidth * (i / 100f));
                    params.height = (int) (defaultHeight * (i / 100f));
                    image.setLayoutParams(params);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new MaterialAlertDialogBuilder(mContext)
                    .setView(dialogView)
                    .setPositiveButton(R.string.action_done, (dialog, which) -> {
                        final int seekProgress = seekBar.getProgress();

                        Prefs.edit()
                                .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                .apply();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setNeutralButton(R.string.action_default, (dialog, which) -> {
                        Prefs.edit().putInt(prefKeyResID, 100).apply();
                    })
                    .show();
        }
    }
}
