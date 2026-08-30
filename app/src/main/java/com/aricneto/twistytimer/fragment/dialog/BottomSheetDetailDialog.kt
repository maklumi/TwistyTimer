package com.aricneto.twistytimer.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aricneto.twistify.databinding.DialogBottomsheetDetailBinding;

public class BottomSheetDetailDialog extends BottomSheetDialogFragment {

    private DialogBottomsheetDetailBinding binding;

    private boolean hasHints = false;

    private String detailText;
    private String hintText;
    private float detailTextSize;

    public static BottomSheetDetailDialog newInstance() {
        return new BottomSheetDetailDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // This makes the bottomSheet dialog start in the expanded state
        dialog.setOnShowListener(dia -> {
            BottomSheetDialog bottomDialog = (BottomSheetDialog) dia;
            FrameLayout bottomSheet =  bottomDialog .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
            BottomSheetBehavior.from(bottomSheet).setHideable(true);
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogBottomsheetDetailBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.detailText.setText(detailText);
        binding.detailText.setTextSize(TypedValue.COMPLEX_UNIT_PX, binding.detailText.getTextSize() * detailTextSize);

        if (!hasHints) {
            setHintVisibility(999);
        } else {
            setHintVisibility(View.GONE);
        }
    }

    public void setDetailText(String text) {
        detailText = text;
    }

    public void setHintText(String text) {
        hintText = text;
        if (binding != null && binding.hintText != null) {
            binding.hintText.setText(hintText);
        }
    }

    public void setDetailTextSize (float size) {
        this.detailTextSize = size;
    }

    public void hasHints(boolean hasHints) {
        this.hasHints = hasHints;
    }

    public void setHintVisibility(int visibility) {
        if (binding != null && binding.hintText != null) {
            if (visibility == View.VISIBLE) {
                binding.hintText.setVisibility(View.VISIBLE);
                binding.hintProgress.setVisibility(View.GONE);
                binding.hintTitle.setVisibility(View.VISIBLE);
                binding.hintDivider.setVisibility(View.VISIBLE);
            } else if (visibility == View.GONE) {
                binding.hintText.setVisibility(View.GONE);
                binding.hintProgress.setVisibility(View.VISIBLE);
                binding.hintTitle.setVisibility(View.VISIBLE);
                binding.hintDivider.setVisibility(View.VISIBLE);
            } else {
                binding.hintProgress.setVisibility(View.GONE);
                binding.hintText.setVisibility(View.GONE);
                binding.hintTitle.setVisibility(View.GONE);
                binding.hintDivider.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
