package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.widget.AppCompatSeekBar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogAlgDetailsBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Shows the algList dialog
 */
public class AlgDialog extends DialogFragment {

    private DialogAlgDetailsBinding binding;
    private Context mContext;

    private long            mId;
    private Algorithm       algorithm;
    private DialogListener  dialogListener;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            switch (view.getId()) {
                case R.id.editButton:
                    View editView = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
                    TextInputEditText editEditText = editView.findViewById(R.id.edit_text);
                    editEditText.setText(algorithm.getAlgs());

                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.edit_algorithm)
                            .setView(editView)
                            .setPositiveButton(R.string.action_done, (dialog1, which) -> {
                                String input = editEditText.getText().toString();
                                algorithm.setAlgs(input);
                                dbHandler.updateAlgorithmAlg(mId, input);
                                binding.algText.setText(input);
                                updateList();
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    break;

                case R.id.progressButton:
                    final AppCompatSeekBar seekBar = (AppCompatSeekBar) LayoutInflater.from(mContext).inflate(R.layout.dialog_progress, null);
                    seekBar.setProgress(algorithm.getProgress());
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.dialog_set_progress)
                            .setView(seekBar)
                            .setPositiveButton(R.string.action_update, (dialog12, which) -> {
                                int seekProgress = seekBar.getProgress();
                                algorithm.setProgress(seekProgress);
                                dbHandler.updateAlgorithmProgress(mId, seekProgress);
                                binding.progressBar.setProgress(seekProgress);
                                updateList();
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    break;

                case R.id.revertButton:
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.dialog_revert_title_confirmation)
                            .setMessage(R.string.dialog_revert_content_confirmation)
                            .setPositiveButton(R.string.action_reset, (dialog13, which) -> {
                                algorithm.setAlgs(AlgUtils.getDefaultAlgs(algorithm.getSubset(), algorithm.getName()));
                                dbHandler.updateAlgorithmAlg(mId, algorithm.getAlgs());
                                binding.algText.setText(algorithm.getAlgs());
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    break;
            }
        }
    };

    public static AlgDialog newInstance(long id) {
        AlgDialog timeDialog = new AlgDialog();
        Bundle args = new Bundle();
        args.putLong("id", id);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogAlgDetailsBinding.inflate(inflater, container, false);

        mContext = getContext();
        mId = getArguments().getLong("id");

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final Algorithm matchedAlgorithm = TwistyTimer.getDBHandler().getAlgorithm(mId);

        if (matchedAlgorithm != null) {
            algorithm = matchedAlgorithm;
            binding.algText.setText(algorithm.getAlgs());
            binding.nameText.setText(algorithm.getName());

            binding.cube.setCubeState(AlgUtils.getCaseState(getContext(), algorithm.getSubset(), algorithm.getName()));

            binding.progressBar.setProgress(algorithm.getProgress());

            binding.revertButton.setOnClickListener(clickListener);
            binding.progressButton.setOnClickListener(clickListener);
            binding.editButton.setOnClickListener(clickListener);

            // If the subset is PLL, it'll need to show the pll arrows.
            if (algorithm.getSubset().equals("PLL")) {
                binding.pllArrows.setImageDrawable(AlgUtils.getPllArrow(getContext(), algorithm.getName()));
                binding.pllArrows.setVisibility(View.VISIBLE);
            }

        }

        return binding.getRoot();
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    private void updateList() {
        TTIntent.broadcast(TTIntent.CATEGORY_ALG_DATA_CHANGES, TTIntent.ACTION_ALGS_MODIFIED);
        //dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (dialogListener != null)
            dialogListener.onDismissDialog();
    }
}
