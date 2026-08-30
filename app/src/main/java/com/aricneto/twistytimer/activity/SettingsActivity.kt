package com.aricneto.twistytimer.activity

import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ActivitySettingsBinding
import com.aricneto.twistytimer.fragment.dialog.CrossHintFaceSelectDialog
import com.aricneto.twistytimer.fragment.dialog.LocaleSelectDialog.Companion.newInstance
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.utils.LocaleUtils.updateLocale
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.Prefs.keyToResourceID
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.ceil

class SettingsActivity : AppCompatActivity() {
    private var binding: ActivitySettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=$savedInstanceState)")

        setTheme(R.style.SettingsTheme)

        updateLocale(applicationContext)

        this.enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.getRoot()) { v: View, insets: WindowInsetsCompat ->
            v.setPadding(
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        binding!!.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (savedInstanceState == null) {
            // Add the main "parent" settings fragment. It is not added to be back stack, so that
            // when "Back" is pressed, the "SettingsActivity" will exit, which is appropriate.
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_container, SettingsFragment(), "fragment_settings")
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val settingsFragment =
                    supportFragmentManager.findFragmentByTag("fragment_settings")

                if (settingsFragment is OnBackPressedInFragmentListener) {
                    if ((settingsFragment as OnBackPressedInFragmentListener).onBackPressedInFragment()) {
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
        try {
            super.onResume()
        } catch (e: ClassCastException) {
            Log.e(TAG, "get life cycle exception", e)
        }
    }

    fun onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): $this")
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: $this")
                ActivityCompat.recreate(this@SettingsActivity)
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    class SettingsFragment : PreferenceFragmentCompat(), OnBackPressedInFragmentListener {
        // Variables used to handle back button behavior
        // Stores last PreferenceScreen opened
        private var lastPreferenceScreen: PreferenceScreen? = null

        // Stores the main PreferenceScreen
        private var mainScreen: PreferenceScreen? = null

         var mContext: Context? = null

        private var inspectionDuration = 0

        private var averageText: String = "average text"

        private val clickListener
                : Preference.OnPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference ->
                when (keyToResourceID(
                    preference.key,
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
                    R.string.pk_timer_animation_duration
                )) {
                    R.string.pk_inspection_time -> createNumberDialog(
                        R.string.inspection_time,
                        R.string.pk_inspection_time
                    )

                    R.string.pk_show_scramble_x_cross_hints -> if (getBoolean(
                            R.string.pk_show_scramble_x_cross_hints,
                            false
                        )
                    ) {
                        MaterialAlertDialogBuilder(mContext!!)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.showHintsXCrossSummary)
                            .setPositiveButton(R.string.action_ok, null)
                            .show()
                    }

                    R.string.pk_options_show_scramble_hints -> CrossHintFaceSelectDialog.newInstance()
                        .show(
                            (activity as AppCompatActivity).supportFragmentManager,
                            "cross_hint_face_dialog"
                        )

                    R.string.pk_locale -> if (activity is AppCompatActivity) {
                        newInstance()
                            .show(
                                (activity as AppCompatActivity).supportFragmentManager,
                                "locale_dialog"
                            )
                    } else {
                        Log.e(TAG, "Could not find correct activity to launch dialog!")
                    }

                    R.string.pk_timer_text_size -> createSeekTextSizeDialog(
                        R.string.pk_timer_text_size,
                        60,
                        "12.34",
                        true
                    )

                    R.string.pk_scramble_image_size -> createImageSeekDialog(
                        R.string.pk_scramble_image_size
                    )

                    R.string.pk_scramble_text_size -> createSeekTextSizeDialog(
                        R.string.pk_scramble_text_size,
                        14, "R U R' U' R' F R2 U' R' U' R U R' F'", false
                    )

                    R.string.pk_advanced_timer_settings_enabled -> if (getBoolean(
                            R.string.pk_advanced_timer_settings_enabled,
                            false
                        )
                    ) {
                        MaterialAlertDialogBuilder(mContext!!)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.advanced_pref_summary)
                            .setPositiveButton(R.string.action_ok, null)
                            .show()
                    }

                    R.string.pk_timer_animation_duration -> createSeekDialog(
                        R.string.pk_timer_animation_duration,
                        0, 1000,
                        R.integer.defaultAnimationDuration,
                        "%d ms"
                    )

                    R.string.pk_stat_trim_size -> {
                        // This would be a lot cleaner with high-order functions, but I couldn't find a way to get it working for API < 24
                        val trimDialogView = LayoutInflater.from(activity)
                            .inflate(R.layout.dialog_settings_progress, null)
                        val trimSeekBar =
                            trimDialogView.findViewById<AppCompatSeekBar>(R.id.seekbar)
                        val trimText = trimDialogView.findViewById<TextView>(R.id.text)

                        val defaultValue = requireContext().resources
                            .getInteger(R.integer.defaultTrimSize)

                        trimSeekBar.max = 30
                        trimSeekBar.progress = getInt(R.string.pk_stat_trim_size, defaultValue)

                        val trimDialog = MaterialAlertDialogBuilder(mContext!!)
                            .setView(trimDialogView)
                            .setPositiveButton(
                                R.string.action_done
                            ) { _: DialogInterface?, _: Int ->
                                edit()
                                    .putInt(
                                        R.string.pk_stat_trim_size,
                                        if (trimSeekBar.progress > 0) trimSeekBar.progress else 0
                                    )
                                    .apply()
                            }
                            .setNegativeButton(R.string.action_cancel, null)
                            .setNeutralButton(
                                R.string.action_default
                            ) { _: DialogInterface?, _: Int ->
                                edit()
                                    .putInt(R.string.pk_stat_trim_size, defaultValue)
                                    .apply()
                            }
                            .create()


                        val trimChangeListener: OnSeekBarChangeListener =
                            object : OnSeekBarChangeListener {
                                var ao50: Int = 0
                                var ao100: Int = 0
                                var ao1000: Int = 0
                                override fun onProgressChanged(
                                    seekBar: SeekBar?,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    ao50 = getTrim(50, progress)
                                    ao100 = getTrim(100, progress)
                                    ao1000 = getTrim(1000, progress)
                                    trimText.text = String.format(
                                        getString(
                                            R.string.pref_dialog_trim_size,
                                            progress
                                        ) + "%%\n\n" +
                                                getString(R.string.pref_dialog_included_solves) +
                                                "\n\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d",
                                        averageText + 3, 0,
                                        averageText + 5, 2,
                                        averageText + 12, 2,
                                        averageText + 50, ao50,
                                        averageText + 100, ao100,
                                        averageText + 1000, ao1000
                                    )
                                }

                                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                                }

                                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                }
                            }

                        trimSeekBar.setOnSeekBarChangeListener(trimChangeListener)
                        trimChangeListener.onProgressChanged(
                            trimSeekBar,
                            trimSeekBar.progress,
                            false
                        )
                        trimDialog.show()
                    }
                }
                false
            }


        override fun onCreate(savedInstanceState: Bundle?) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            super.onCreate(savedInstanceState)

            mContext = requireContext()

            averageText = getString(R.string.graph_legend_avg_prefix)
        }

        override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)

            val listenerPrefIds: IntArray = intArrayOf(
                R.string.pk_inspection_time,
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
                R.string.pk_timer_animation_duration
            )

            for (prefId in listenerPrefIds) {
                val preference = findPreference<Preference?>(getString(prefId))
                preference?.onPreferenceClickListener = clickListener
            }

            mainScreen = preferenceScreen
        }

        override fun onResume() {
            super.onResume()
            // Set the Inspection Alert preference summary to display the correct information
            // about time elapsed depending on user's current inspection duration
            updateInspectionAlertText()
        }

        private fun updateInspectionAlertText() {
            inspectionDuration = getInt(R.string.pk_inspection_time, 15)

            val inspectionPreference =
                findPreference<Preference?>(getString(R.string.pk_inspection_alert_enabled))
            inspectionPreference?.setSummary(
                getString(
                    R.string.pref_inspection_alert_summary,
                    if (inspectionDuration == 15) 8 else (inspectionDuration * 0.5f).toInt(),
                    if (inspectionDuration == 15) 12 else (inspectionDuration * 0.8f).toInt()
                )
            )
        }

        override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
            lastPreferenceScreen = getPreferenceScreen()
            setPreferenceScreen(preferenceScreen)
        }

        /**
         * PreferenceFragmentCompat does not handle back button behavior by default.
         * To implement the correct behavior, we store the last opened [PreferenceScreen] and
         * the main [PreferenceScreen]. When back button is pressed, we check if the current
         * screen is the same as mainScreen, if it is, we return false so the Activity can handle
         * it by closing [SettingsActivity]. If not, we set our screen to the last one we opened
         * 
         * @return true if back button was consumed
         */
        override fun onBackPressedInFragment(): Boolean {
            if (lastPreferenceScreen != null && preferenceScreen != lastPreferenceScreen) {
                preferenceScreen = lastPreferenceScreen
                return true
            }
            return false
        }

        private fun createNumberDialog(@StringRes title: Int, prefKeyResID: Int) {
            val view = LayoutInflater.from(mContext!!).inflate(R.layout.dialog_input, null)
            val editText = view.findViewById<TextInputEditText>(R.id.edit_text)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setText(getInt(prefKeyResID, 15).toString())

            MaterialAlertDialogBuilder(mContext!!)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(
                    R.string.action_done
                ) { _: DialogInterface?, _: Int ->
                    try {
                        val time = editText.getText().toString().toInt()
                        edit().putInt(prefKeyResID, time).apply()
                    } catch (_: NumberFormatException) {
                        Toast.makeText(activity, R.string.invalid_time, Toast.LENGTH_SHORT)
                            .show()
                    }
                    updateInspectionAlertText()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(
                    R.string.action_default
                ) { _: DialogInterface?, _: Int ->
                    edit().putInt(prefKeyResID, 15).apply()
                    updateInspectionAlertText()
                }
                .show()
        }

        private fun createSeekDialog(
            @StringRes prefKeyResID: Int,
            minValue: Int, maxValue: Int, @IntegerRes defaultValueRes: Int,
            formatText: String
        ) {
            val dialogView =
                LayoutInflater.from(activity).inflate(R.layout.dialog_settings_progress, null)
            val seekBar = dialogView.findViewById<AppCompatSeekBar>(R.id.seekbar)
            val text = dialogView.findViewById<TextView>(R.id.text)

            val defaultValue = requireContext().resources.getInteger(defaultValueRes)

            seekBar.max = maxValue
            seekBar.progress = getInt(prefKeyResID, defaultValue)

            text.text = String.format(formatText, seekBar.progress)

            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    text.text = String.format(formatText, seekBar.progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })

            MaterialAlertDialogBuilder(mContext!!)
                .setView(dialogView)
                .setPositiveButton(
                    R.string.action_done
                ) { _: DialogInterface?, _: Int ->
                    val seekProgress = seekBar.progress
                    edit()
                        .putInt(
                            prefKeyResID,
                            if (seekProgress > minValue) seekProgress else minValue
                        )
                        .apply()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(
                    R.string.action_default
                ) { _: DialogInterface?, _: Int ->
                    edit().putInt(
                        prefKeyResID,
                        defaultValue
                    ).apply()
                }
                .show()
        }

        private fun getTrim(avg: Int, trim: Int): Int {
            return (ceil((avg * (trim / 100f)).toDouble()).toInt() * 2)
        }

        private fun createSeekTextSizeDialog(
            prefKeyResID: Int, defaultTextSize: Int, showText: String?, bold: Boolean
        ) {
            val dialogView =
                LayoutInflater.from(activity).inflate(R.layout.dialog_settings_progress, null)
            val seekBar = dialogView.findViewById<View?>(R.id.seekbar) as AppCompatSeekBar
            val text = dialogView.findViewById<View?>(R.id.text) as TextView
            seekBar.max = 300
            seekBar.progress = getInt(prefKeyResID, 100)

            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize.toFloat())
            val defaultTextSizePx = text.textSize
            text.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                defaultTextSizePx * (seekBar.progress / 100f)
            )
            if (bold) text.setTypeface(Typeface.DEFAULT_BOLD)
            text.text = showText

            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (i / 100f))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })

            MaterialAlertDialogBuilder(mContext!!)
                .setView(dialogView)
                .setPositiveButton(
                    R.string.action_done
                ) { _: DialogInterface?, _: Int ->
                    val seekProgress = seekBar.progress
                    edit()
                        .putInt(prefKeyResID, if (seekProgress > 10) seekProgress else 10)
                        .apply()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(
                    R.string.action_default
                ) { _: DialogInterface?, _: Int ->
                    edit().putInt(prefKeyResID, 100).apply()
                }
                .show()
        }

        private fun createImageSeekDialog(prefKeyResID: Int) {
            val dialogView = LayoutInflater.from(
                activity
            ).inflate(R.layout.dialog_settings_progress_image, null)
            val seekBar = dialogView.findViewById<View?>(R.id.seekbar) as AppCompatSeekBar
            val image = dialogView.findViewById<View>(R.id.image)
            seekBar.max = 300
            seekBar.progress = getInt(prefKeyResID, 100)

            val defaultWidth = image.layoutParams.width
            val defaultHeight = image.layoutParams.height

            image.layoutParams.width =
                (image.layoutParams.width * (seekBar.progress / 100f)).toInt()
            image.layoutParams.height =
                (image.layoutParams.height * (seekBar.progress / 100f)).toInt()


            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                    val params = image.layoutParams as LinearLayout.LayoutParams
                    params.width = (defaultWidth * (i / 100f)).toInt()
                    params.height = (defaultHeight * (i / 100f)).toInt()
                    image.layoutParams = params
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })

            MaterialAlertDialogBuilder(mContext!!)
                .setView(dialogView)
                .setPositiveButton(
                    R.string.action_done
                ) { _: DialogInterface?, _: Int ->
                    val seekProgress = seekBar.progress
                    edit()
                        .putInt(prefKeyResID, if (seekProgress > 10) seekProgress else 10)
                        .apply()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(
                    R.string.action_default
                ) { _: DialogInterface?, _: Int ->
                    edit().putInt(prefKeyResID, 100).apply()
                }
                .show()
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
        private val TAG: String = SettingsActivity::class.java.simpleName
    }
}
