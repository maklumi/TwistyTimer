package com.aricneto.twistytimer.fragment.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogAddTimeBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.watcher.SolveTimeNumberTextWatcher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.joda.time.DateTime;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED_MANUALLY;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

/**
 * Shows the algList dialog
 */
public class AddTimeDialog extends DialogFragment {

    private DialogAddTimeBinding binding;

    private DialogListener  dialogListener;

    private String currentPuzzle;
    private String currentScramble;
    private String currentPuzzleSubtype;

    private int mCurrentPenalty = PuzzleUtils.NO_PENALTY;
    private String mCurrentComment = "";

    private Context mContext;

    @SuppressLint("RestrictedApi")
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.button_save:
                    if (binding.editTextTime.getText().toString().length() > 0) {
                        int time = (int) PuzzleUtils.parseAddedTime(binding.editTextTime.getText().toString());
                        final Solve solve = new Solve(
                                mCurrentPenalty == PuzzleUtils.PENALTY_PLUSTWO ? time + 2_000 : time,
                                currentPuzzle,
                                currentPuzzleSubtype,
                                new DateTime().getMillis(),
                                binding.checkScramble.isChecked() ? currentScramble : "",
                                mCurrentPenalty,
                                mCurrentComment,
                                false);

                        TwistyTimer.getDBHandler().addSolve(solve);
                        // The receiver might be able to use the new solve and avoid
                        // accessing the database.
                        new TTIntent.BroadcastBuilder(CATEGORY_UI_INTERACTIONS, ACTION_TIME_ADDED_MANUALLY)
                                .solve(solve)
                                .broadcast();

                        // Generate new scramble
                        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);

                        dismiss();
                    } else {
                        dismiss();
                    }
                    break;
                case R.id.button_more:
                    PopupMenu popupMenu = new PopupMenu(mContext, binding.buttonMore);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_add_time_options, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.penalty:
                                new MaterialAlertDialogBuilder(mContext)
                                        .setTitle(R.string.select_penalty)
                                        .setSingleChoiceItems(R.array.array_penalties, mCurrentPenalty, (dialog, which) -> {
                                            switch (which) {
                                                case 0: // No penalty
                                                    mCurrentPenalty = PuzzleUtils.NO_PENALTY;
                                                    break;
                                                case 1: // +2
                                                    mCurrentPenalty = PuzzleUtils.PENALTY_PLUSTWO;
                                                    break;
                                                case 2: // DNF
                                                    mCurrentPenalty = PuzzleUtils.PENALTY_DNF;
                                                    break;
                                            }
                                            dialog.dismiss();
                                        })
                                        .setNegativeButton(R.string.action_cancel, null)
                                        .show();
                                break;
                            case R.id.comment:
                                View commentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
                                TextInputEditText commentEditText = commentView.findViewById(R.id.edit_text);
                                commentEditText.setText(mCurrentComment);

                                new MaterialAlertDialogBuilder(mContext)
                                        .setTitle(R.string.edit_comment)
                                        .setView(commentView)
                                        .setPositiveButton(R.string.action_done, (dialog, which) -> {
                                            mCurrentComment = commentEditText.getText().toString();
                                        })
                                        .setNegativeButton(R.string.action_cancel, null)
                                        .show();
                                break;
                        }
                        return true;
                    });

                    MenuPopupHelper popupHelper = new MenuPopupHelper(mContext, (MenuBuilder) popupMenu.getMenu(), binding.buttonMore);
                    popupHelper.setForceShowIcon(true);
                    popupHelper.show();
                    break;
            }
        }
    };

    public static AddTimeDialog newInstance(String currentPuzzle, String currentPuzzleSubtype, String currentScramble) {
        AddTimeDialog timeDialog = new AddTimeDialog();
        Bundle args = new Bundle();
        args.putString("puzzle", currentPuzzle);
        args.putString("category", currentPuzzleSubtype);
        args.putString("scramble", currentScramble);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        currentPuzzle = getArguments().getString("puzzle");
        currentPuzzleSubtype = getArguments().getString("category");
        currentScramble = getArguments().getString("scramble");
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogAddTimeBinding.inflate(inflater, container, false);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        binding.buttonSave.setOnClickListener(clickListener);
        binding.buttonMore.setOnClickListener(clickListener);
        binding.editTextTime.addTextChangedListener(new SolveTimeNumberTextWatcher());

        // Focus on editText and request keyboard
        binding.editTextTime.requestFocus();

        try {
            binding.editTextTime.postDelayed(() ->
                    ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(binding.editTextTime, InputMethodManager.SHOW_IMPLICIT), 400);
        } catch (Exception e) {
            Log.e("AddTimeDialog", "Error showing keyboard: " + e);
        }

        return binding.getRoot();
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Hide keyboard
        try {
            ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(binding.editTextTime.getWindowToken(), 0);
        } catch (Exception e) {
            Log.e("AddTimeDialog", "Error hiding keyboard: " + e);
        }

        if (dialogListener != null)
            dialogListener.onDismissDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
