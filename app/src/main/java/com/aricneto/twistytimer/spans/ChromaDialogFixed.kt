package com.aricneto.twistytimer.spans

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar

/**
 * Replacement for ChromaDialog using ColorPickerView
 */
class ChromaDialogFixed : DialogFragment() {
    interface OnColorSelectedListener {
        fun onColorSelected(@ColorInt color: Int)
    }

    // Stubs for compatibility with old code
    enum class ColorMode {
        RGB, HSV, ARGB, CMYK
    }

    enum class IndicatorMode {
        HEX, DECIMAL
    }

    private var listener: OnColorSelectedListener? = null

    class Builder {
        @ColorInt
        private var initialColor = -0x1
        private var listener: OnColorSelectedListener? = null

        fun initialColor(@ColorInt initialColor: Int): Builder {
            this.initialColor = initialColor
            return this
        }

        // Methods for compatibility, currently ignored as skydoves has different config
        fun colorMode(colorMode: ColorMode?): Builder {
            return this
        }

        fun indicatorMode(indicatorMode: IndicatorMode?): Builder {
            return this
        }

        fun onColorSelected(listener: OnColorSelectedListener?): Builder {
            this.listener = listener
            return this
        }

        fun create(): ChromaDialogFixed {
            val fragment: ChromaDialogFixed = newInstance(initialColor)
            fragment.setListener(listener)
            return fragment
        }
    }

    fun setListener(listener: OnColorSelectedListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val initialColor =
            if (arguments != null) requireArguments().getInt(ARG_INITIAL_COLOR) else -0x1

        val density = resources.displayMetrics.density
        val pickerSize = (260 * density).toInt()
        val margin = (16 * density).toInt()

        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL
        container.setPadding(margin, margin, margin, margin)

        val brightnessSlideBar = BrightnessSlideBar(requireContext())
        val sliderParams = LinearLayout.LayoutParams(
            pickerSize,
            (32 * density).toInt()
        )
        sliderParams.topMargin = margin
        brightnessSlideBar.layoutParams = sliderParams

        val colorPickerView = ColorPickerView.Builder(requireContext())
            .setInitialColor(initialColor)
            .setWidth(260)
            .setHeight(260)
            .setSelectorSize(20)
            .setBrightnessSlideBar(brightnessSlideBar)
            .build()

        container.addView(colorPickerView)
        container.addView(brightnessSlideBar)

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.color_picker_title)
            .setView(container)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    listener?.onColorSelected(colorPickerView.color)
                })
            .setNegativeButton(
                android.R.string.cancel,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dismiss() })
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener = null
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "arg_initial_color"

        fun newInstance(@ColorInt initialColor: Int): ChromaDialogFixed {
            val fragment = ChromaDialogFixed()
            val args = Bundle()
            args.putInt(ARG_INITIAL_COLOR, initialColor)
            fragment.setArguments(args)
            return fragment
        }
    }
}
