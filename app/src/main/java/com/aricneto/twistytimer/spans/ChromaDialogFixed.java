package com.aricneto.twistytimer.spans;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.aricneto.twistify.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

/**
 * Replacement for ChromaDialog using ColorPickerView
 */
public class ChromaDialogFixed extends DialogFragment {

    private final static String ARG_INITIAL_COLOR = "arg_initial_color";

    public interface OnColorSelectedListener {
        void onColorSelected(@ColorInt int color);
    }

    // Stubs for compatibility with old code
    public enum ColorMode { RGB, HSV, ARGB, CMYK }
    public enum IndicatorMode { HEX, DECIMAL }

    private OnColorSelectedListener listener;

    public static ChromaDialogFixed newInstance(@ColorInt int initialColor) {
        ChromaDialogFixed fragment = new ChromaDialogFixed();
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_COLOR, initialColor);
        fragment.setArguments(args);
        return fragment;
    }

    public static class Builder {
        private @ColorInt int initialColor = 0xFFFFFFFF;
        private OnColorSelectedListener listener = null;

        public Builder initialColor(@ColorInt int initialColor) {
            this.initialColor = initialColor;
            return this;
        }

        // Methods for compatibility, currently ignored as skydoves has different config
        public Builder colorMode(ColorMode colorMode) { return this; }
        public Builder indicatorMode(IndicatorMode indicatorMode) { return this; }

        public Builder onColorSelected(OnColorSelectedListener listener) {
            this.listener = listener;
            return this;
        }

        public ChromaDialogFixed create() {
            ChromaDialogFixed fragment = newInstance(initialColor);
            fragment.setListener(listener);
            return fragment;
        }
    }

    public void setListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int initialColor = getArguments() != null ? getArguments().getInt(ARG_INITIAL_COLOR) : 0xFFFFFFFF;

        float density = getResources().getDisplayMetrics().density;
        int pickerSize = (int) (260 * density);
        int margin = (int) (16 * density);

        LinearLayout container = new LinearLayout(getActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(margin, margin, margin, margin);

        BrightnessSlideBar brightnessSlideBar = new BrightnessSlideBar(getActivity());
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                pickerSize,
                (int) (32 * density)
        );
        sliderParams.topMargin = margin;
        brightnessSlideBar.setLayoutParams(sliderParams);

        ColorPickerView colorPickerView = new ColorPickerView.Builder(getActivity())
                .setInitialColor(initialColor)
                .setWidth(260)
                .setHeight(260)
                .setSelectorSize(20)
                .setBrightnessSlideBar(brightnessSlideBar)
                .build();
        
        container.addView(colorPickerView);
        container.addView(brightnessSlideBar);

        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.color_picker_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (listener != null) {
                        listener.onColorSelected(colorPickerView.getColor());
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss())
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}
