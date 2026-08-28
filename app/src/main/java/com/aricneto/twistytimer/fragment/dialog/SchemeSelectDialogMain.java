package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogSchemeSelectMainBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.spans.ChromaDialogFixed;
import com.aricneto.twistytimer.spans.ChromaDialogFixed.OnColorSelectedListener;
import com.aricneto.twistytimer.spans.ChromaDialogFixed.ColorMode;
import com.aricneto.twistytimer.spans.ChromaDialogFixed.IndicatorMode;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Created by Ari on 09/02/2016.
 */
public class SchemeSelectDialogMain extends DialogFragment {

    private DialogSchemeSelectMainBinding binding;
    private Context mContext;

    public static SchemeSelectDialogMain newInstance() {
        return new SchemeSelectDialogMain();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());
            final SharedPreferences.Editor editor = sp.edit();
            String currentHex = "FFFFFF";
            switch (view.getId()) {
                case R.id.top:
                    currentHex = sp.getString("cubeTop", "FFFFFF");
                    break;
                case R.id.left:
                    currentHex = sp.getString("cubeLeft", "FF8B24");
                    break;
                case R.id.front:
                    currentHex = sp.getString("cubeFront", "02D040");
                    break;
                case R.id.right:
                    currentHex = sp.getString("cubeRight", "EC0000");
                    break;
                case R.id.back:
                    currentHex = sp.getString("cubeBack", "304FFE");
                    break;
                case R.id.down:
                    currentHex = sp.getString("cubeDown", "FDD835");
                    break;
            }

            new ChromaDialogFixed.Builder()
                    .initialColor(Color.parseColor("#" + currentHex))
                    .colorMode(ColorMode.RGB)
                    .indicatorMode(IndicatorMode.HEX)
                    .onColorSelected(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(@ColorInt int color) {
                            String hexColor = Integer.toHexString(color).toUpperCase().substring(2);
                            switch (view.getId()) {
                                case R.id.top:
                                    setColor(binding.top, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeTop", hexColor);
                                    break;
                                case R.id.left:
                                    setColor(binding.left, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeLeft", hexColor);
                                    break;
                                case R.id.front:
                                    setColor(binding.front, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeFront", hexColor);
                                    break;
                                case R.id.right:
                                    setColor(binding.right, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeRight", hexColor);
                                    break;
                                case R.id.back:
                                    setColor(binding.back, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeBack", hexColor);
                                    break;
                                case R.id.down:
                                    setColor(binding.down, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeDown", hexColor);
                                    break;
                            }
                            editor.apply();
                        }
                    })
                    .create()
                    .show(getFragmentManager(), "ChromaDialog");

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogSchemeSelectMainBinding.inflate(inflater, container, false);

        mContext = getContext();

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());

        setColor(binding.top, Color.parseColor("#" + sp.getString("cubeTop", "FFFFFF")));
        setColor(binding.left, Color.parseColor("#" + sp.getString("cubeLeft", "FF8B24")));
        setColor(binding.front, Color.parseColor("#" + sp.getString("cubeFront", "02D040")));
        setColor(binding.right, Color.parseColor("#" + sp.getString("cubeRight", "EC0000")));
        setColor(binding.back, Color.parseColor("#" + sp.getString("cubeBack", "304FFE")));
        setColor(binding.down, Color.parseColor("#" + sp.getString("cubeDown", "FDD835")));

        binding.top.setOnClickListener(clickListener);
        binding.left.setOnClickListener(clickListener);
        binding.front.setOnClickListener(clickListener);
        binding.right.setOnClickListener(clickListener);
        binding.back.setOnClickListener(clickListener);
        binding.down.setOnClickListener(clickListener);

        binding.reset.setOnClickListener(view -> new MaterialAlertDialogBuilder(mContext)
                .setMessage(R.string.reset_colorscheme)
                .setPositiveButton(R.string.action_reset_colorscheme, (dialog, which) -> {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("cubeTop", "FFFFFF");
                    editor.putString("cubeLeft", "EF6C00");
                    editor.putString("cubeFront", "02D040");
                    editor.putString("cubeRight", "EC0000");
                    editor.putString("cubeBack", "304FFE");
                    editor.putString("cubeDown", "FDD835");
                    editor.apply();
                    setColor(binding.top, Color.parseColor("#FFFFFF"));
                    setColor(binding.left, Color.parseColor("#EF6C00"));
                    setColor(binding.front, Color.parseColor("#02D040"));
                    setColor(binding.right, Color.parseColor("#EC0000"));
                    setColor(binding.back, Color.parseColor("#304FFE"));
                    setColor(binding.down, Color.parseColor("#FDD835"));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show());

        binding.done.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onRecreateRequired();
            }
            dismiss();
        });

        return binding.getRoot();
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
