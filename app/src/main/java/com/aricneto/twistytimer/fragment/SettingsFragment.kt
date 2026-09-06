package com.aricneto.twistytimer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.aricneto.twistify.R
import com.aricneto.twistytimer.fragment.dialog.CrossHintFaceSelectDialog
import com.aricneto.twistytimer.fragment.dialog.LocaleSelectDialog
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.Prefs.keyToResourceID
import com.aricneto.twistytimer.utils.ThemeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.math.ceil

class SettingsFragment : PreferenceFragmentCompat(), OnBackPressedInFragmentListener {

    private var mContext: Context? = null
    private var inspectionDuration = 0
    private var averageText: String = "average text"

    private val clickListener: Preference.OnPreferenceClickListener =
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
                    MaterialAlertDialogBuilder(requireActivity())
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
                    LocaleSelectDialog.newInstance()
                        .show(
                            (activity as AppCompatActivity).supportFragmentManager,
                            "locale_dialog"
                        )
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
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.warning)
                        .setMessage(R.string.advanced_pref_summary)
                        .setPositiveButton(R.string.action_ok, null)
                        .show()
                }

                R.string.pk_timer_animation_duration -> createSeekDialog(
                    prefKeyResID = R.string.pk_timer_animation_duration,
                    defaultValueRes = R.integer.defaultAnimationDuration
                )

                R.string.pk_stat_trim_size -> {
                    @SuppressLint("InflateParams")
                    val trimDialogView = layoutInflater.inflate(R.layout.dialog_settings_progress, null)
                    val trimSeekBar = trimDialogView.findViewById<SeekBar>(R.id.seekbar)
                    val trimText = trimDialogView.findViewById<TextView>(R.id.text)

                    val defaultValue = requireContext().resources.getInteger(R.integer.defaultTrimSize)

                    trimSeekBar.max = 30
                    trimSeekBar.progress = getInt(R.string.pk_stat_trim_size, defaultValue)

                    val trimDialog = MaterialAlertDialogBuilder(requireActivity())
                        .setView(trimDialogView)
                        .setPositiveButton(R.string.action_done) { _: DialogInterface?, _: Int ->
                            edit().putInt(R.string.pk_stat_trim_size, if (trimSeekBar.progress > 0) trimSeekBar.progress else 0).apply()
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .setNeutralButton(R.string.action_default) { _: DialogInterface?, _: Int ->
                            edit().putInt(R.string.pk_stat_trim_size, defaultValue).apply()
                        }
                        .create()

                    val trimChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
                        var ao50: Int = 0
                        var ao100: Int = 0
                        var ao1000: Int = 0
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            ao50 = getTrim(50, progress)
                            ao100 = getTrim(100, progress)
                            ao1000 = getTrim(1000, progress)
                            trimText.text = String.format(
                                Locale.getDefault(),
                                getString(R.string.pref_dialog_trim_size, progress) + "%%\n\n" +
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
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    }

                    trimSeekBar.setOnSeekBarChangeListener(trimChangeListener)
                    trimChangeListener.onProgressChanged(trimSeekBar, trimSeekBar.progress, false)
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

        val listenerPrefIds = intArrayOf(
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

        tintIcons(preferenceScreen)
    }

    private fun tintIcons(preference: Preference) {
        val icon = preference.icon
        icon?.setTint(ThemeUtils.fetchAttrColor(requireContext(), R.attr.colorOnSurface))

        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                tintIcons(preference.getPreference(i))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateInspectionAlertText()
    }

    private fun updateInspectionAlertText() {
        inspectionDuration = getInt(R.string.pk_inspection_time, 15)
        val inspectionPreference = findPreference<Preference?>(getString(R.string.pk_inspection_alert_enabled))
        inspectionPreference?.setSummary(
            getString(
                R.string.pref_inspection_alert_summary,
                if (inspectionDuration == 15) 8 else (inspectionDuration * 0.5f).toInt(),
                if (inspectionDuration == 15) 12 else (inspectionDuration * 0.8f).toInt()
            )
        )
    }

    override fun onBackPressedInFragment(): Boolean {
        return false // Handled by Activity via back stack
    }

    private fun createNumberDialog(@StringRes title: Int, prefKeyResID: Int) {
        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val editText = view.findViewById<TextInputEditText>(R.id.edit_text)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.setText(getInt(prefKeyResID, 15).toString())

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.action_done) { _: DialogInterface?, _: Int ->
                try {
                    val time = editText.text.toString().toInt()
                    edit().putInt(prefKeyResID, time).apply()
                } catch (_: NumberFormatException) {
                    Toast.makeText(activity, R.string.invalid_time, Toast.LENGTH_SHORT).show()
                }
                updateInspectionAlertText()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_default) { _: DialogInterface?, _: Int ->
                edit().putInt(prefKeyResID, 15).apply()
                updateInspectionAlertText()
            }
            .show()
    }

    private fun createSeekDialog(
        @StringRes prefKeyResID: Int,
        minValue: Int = 0, maxValue: Int = 1000, @IntegerRes defaultValueRes: Int,
        formatText: String = "%d ms"
    ) {
        @SuppressLint("InflateParams")
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings_progress, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekbar)
        val text = dialogView.findViewById<TextView>(R.id.text)
        val defaultValue = requireContext().resources.getInteger(defaultValueRes)
        seekBar.max = maxValue
        seekBar.progress = getInt(prefKeyResID, defaultValue)
        text.text = String.format(Locale.getDefault(), formatText, seekBar.progress)

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                text.text = String.format(Locale.getDefault(), formatText, seekBar.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        MaterialAlertDialogBuilder(requireActivity())
            .setView(dialogView)
            .setPositiveButton(R.string.action_done) { _: DialogInterface?, _: Int ->
                val seekProgress = seekBar.progress
                edit().putInt(prefKeyResID, if (seekProgress > minValue) seekProgress else minValue).apply()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_default) { _: DialogInterface?, _: Int ->
                edit().putInt(prefKeyResID, defaultValue).apply()
            }
            .show()
    }

    private fun getTrim(avg: Int, trim: Int): Int {
        return (ceil((avg * (trim / 100f)).toDouble()).toInt() * 2)
    }

    private fun createSeekTextSizeDialog(
        prefKeyResID: Int, defaultTextSize: Int, showText: String?, bold: Boolean
    ) {
        @SuppressLint("InflateParams")
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings_progress, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekbar)
        val text = dialogView.findViewById<TextView>(R.id.text)
        seekBar.max = 300
        seekBar.progress = getInt(prefKeyResID, 100)

        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize.toFloat())
        val defaultTextSizePx = text.textSize
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (seekBar.progress / 100f))
        if (bold) text.setTypeface(Typeface.DEFAULT_BOLD)
        text.text = showText

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (i / 100f))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        MaterialAlertDialogBuilder(requireActivity())
            .setView(dialogView)
            .setPositiveButton(R.string.action_done) { _: DialogInterface?, _: Int ->
                val seekProgress = seekBar.progress
                edit().putInt(prefKeyResID, if (seekProgress > 10) seekProgress else 10).apply()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_default) { _: DialogInterface?, _: Int ->
                edit().putInt(prefKeyResID, 100).apply()
            }
            .show()
    }

    private fun createImageSeekDialog(prefKeyResID: Int) {
        @SuppressLint("InflateParams")
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings_progress_image, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekbar)
        val image = dialogView.findViewById<View>(R.id.image)
        seekBar.max = 420
        seekBar.progress = getInt(prefKeyResID, 180)

        val defaultWidth = image.layoutParams.width     // 84dp
        val defaultHeight = image.layoutParams.height   // 84dp

        image.layoutParams.width = (image.layoutParams.width * (seekBar.progress / 100f)).toInt()
        image.layoutParams.height = (image.layoutParams.height * (seekBar.progress / 100f)).toInt()

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                val params = image.layoutParams as LinearLayout.LayoutParams
                params.width = (defaultWidth * (i / 100f)).toInt()
                params.height = (defaultHeight * (i / 100f)).toInt()
                image.layoutParams = params
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        MaterialAlertDialogBuilder(requireActivity())
            .setView(dialogView)
            .setPositiveButton(R.string.action_done) { _: DialogInterface?, _: Int ->
                val seekProgress = seekBar.progress
                edit().putInt(prefKeyResID, if (seekProgress > 10) seekProgress else 10).apply()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_default) { _: DialogInterface?, _: Int ->
                edit().putInt(prefKeyResID, 100).apply()
            }
            .show()
    }
}
