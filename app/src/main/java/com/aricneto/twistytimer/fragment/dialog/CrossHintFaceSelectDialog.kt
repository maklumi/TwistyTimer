package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogCrossHintFaceSelectBinding;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.DefaultPrefs;
import com.aricneto.twistytimer.utils.Prefs;

/**
 * Dialog that allows a user to select the faces where the cross hints will be shown
 */

public class CrossHintFaceSelectDialog extends DialogFragment {
    private DialogCrossHintFaceSelectBinding binding;

    public static CrossHintFaceSelectDialog newInstance() {
        return new CrossHintFaceSelectDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    // Number of faces that have hint enabled.
    // The code checks if it is >= 1 so the user can't have 0 faces enabled
    // (which would result in a counter-intuitive empty hint)
    int facesSelected = 0;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {

            boolean isHintOn = true;

            switch (view.getId()) {
                case R.id.top:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled));
                    break;
                case R.id.left:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled));
                    break;
                case R.id.front:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled));
                    break;
                case R.id.right:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled));
                    break;
                case R.id.back:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled));
                    break;
                case R.id.down:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled));
                    break;
            }

            isHintOn = !isHintOn;

            if (!(!isHintOn && facesSelected == 1)) {
                switch (view.getId()) {
                case R.id.top:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_top_enabled, isHintOn).apply();
                        toggleSelected(binding.top, isHintOn);
                        break;
                    case R.id.left:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_left_enabled, isHintOn).apply();
                        toggleSelected(binding.left, isHintOn);
                        break;
                    case R.id.front:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_front_enabled, isHintOn).apply();
                        toggleSelected(binding.front, isHintOn);
                        break;
                    case R.id.right:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_right_enabled, isHintOn).apply();
                        toggleSelected(binding.right, isHintOn);
                        break;
                    case R.id.back:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_back_enabled, isHintOn).apply();
                        toggleSelected(binding.back, isHintOn);
                        break;
                    case R.id.down:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_down_enabled, isHintOn).apply();
                        toggleSelected(binding.down, isHintOn);
                        break;
                }
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogCrossHintFaceSelectBinding.inflate(inflater, container, false);

        // Color cube
        setColor(binding.top, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_top_color, "FFFFFF")));
        setColor(binding.left, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_left_color, "FF8B24")));
        setColor(binding.front, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_front_color, "02D040")));
        setColor(binding.right, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_right_color, "EC0000")));
        setColor(binding.back, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_back_color, "304FFE")));
        setColor(binding.down, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_down_color, "FDD835")));

        // Set click listeners
        binding.top.setOnClickListener(clickListener);
        binding.left.setOnClickListener(clickListener);
        binding.front.setOnClickListener(clickListener);
        binding.right.setOnClickListener(clickListener);
        binding.back.setOnClickListener(clickListener);
        binding.down.setOnClickListener(clickListener);

        // Set face transparency based on current preference
        initSelected(binding.top, Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled)));
        initSelected(binding.left, Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled)));
        initSelected(binding.front, Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled)));
        initSelected(binding.right, Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled)));
        initSelected(binding.back, Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled)));
        initSelected(binding.down, Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled)));

        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onRecreateRequired();
                }
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return binding.getRoot();
    }

    /**
     * Initializes the selected faces and facesSelected count
     * Since facesSelected starts at 0, adding 1 to each face that has hints on will start
     * facesSelected on the correct number. If you were to initialize with toggleSelected, the count
     * will probably be wrong (or negative even)
     * @param face
     *      The face to toggle alpha
     * @param isHintOn
     *      True if face has hints enabled
     */
    private void initSelected(View face, boolean isHintOn) {
        if (isHintOn) {
            face.setAlpha(1f);
            facesSelected++;
        } else {
            face.setAlpha(0.2f);
        }
    }

    /**
     * Toggles alpha value on face and changes faceSelected counter
     *
     * @param face
     *      The face to toggle alpha
     * @param isHintOn
     *      True if face has hints enabled
     */
    private void toggleSelected(View face, boolean isHintOn) {
        if (isHintOn) {
            face.setAlpha(1f);
            facesSelected++;
        } else {
            face.setAlpha(0.2f);
            facesSelected--;
        }
    }

    private void setColor(View view, int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.square);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, color);
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();
        view.setBackground(wrap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
