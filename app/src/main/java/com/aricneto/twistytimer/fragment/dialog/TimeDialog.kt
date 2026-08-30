package com.aricneto.twistytimer.fragment.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.widget.PopupMenu;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogTimeDetailsBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AnimUtils;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.joda.time.DateTime;

/**
 * Shows the timeList dialog
 */
public class TimeDialog extends DialogFragment {

    private DialogTimeDetailsBinding binding;

    private long            mId;
    private Solve           solve;
    private DialogListener  dialogListener;

    @SuppressLint("RestrictedApi")
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            switch (view.getId()) {
                case R.id.overflowButton:
                    PopupMenu popupMenu = new PopupMenu(getActivity(), binding.overflowButton);
                    if (solve.isHistory())
                        popupMenu.getMenuInflater().inflate(R.menu.menu_list_detail_history, popupMenu.getMenu());
                    else
                        popupMenu.getMenuInflater().inflate(R.menu.menu_list_detail, popupMenu.getMenu());

                    MenuPopupHelper popupHelper = new MenuPopupHelper(mContext, (MenuBuilder) popupMenu.getMenu(), binding.overflowButton);
                    popupHelper.setForceShowIcon(true);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.share:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, PuzzleUtils.convertTimeToString(solve.getTime(), PuzzleUtils.FORMAT_DEFAULT) + "s.\n" + solve.getComment() + "\n" + solve.getScramble());
                                    shareIntent.setType("text/plain");
                                    getContext().startActivity(shareIntent);
                                    break;
                                case R.id.remove:
                                    dbHandler.deleteSolveByID(mId);
                                    updateList();
                                    break;
                                case R.id.history_to:
                                    solve.setHistory(true);
                                    Toast.makeText(getContext(), getString(R.string.sent_to_history), Toast.LENGTH_SHORT).show();
                                    dbHandler.updateSolve(solve);
                                    updateList();
                                    dismiss();
                                    break;
                                case R.id.history_from:
                                    solve.setHistory(false);
                                    Toast.makeText(getContext(), getString(R.string.sent_to_session), Toast.LENGTH_SHORT).show();
                                    dbHandler.updateSolve(solve);
                                    updateList();
                                    dismiss();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupHelper.show();
                    break;
                case R.id.editButton:
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.select_penalty)
                            .setSingleChoiceItems(R.array.array_penalties, solve.getPenalty(), (dialog, which) -> {
                                switch (which) {
                                    case 0: // No penalty
                                        solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.NO_PENALTY);
                                        break;
                                    case 1: // +2
                                        solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.PENALTY_PLUSTWO);
                                        break;
                                    case 2: // DNF
                                        solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.PENALTY_DNF);
                                        break;
                                }
                                dbHandler.updateSolve(solve);
                                updateList();
                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    break;
                case R.id.commentButton:
                    View commentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
                    TextInputEditText commentEditText = commentView.findViewById(R.id.edit_text);
                    commentEditText.setText(solve.getComment());

                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.edit_comment)
                            .setView(commentView)
                            .setPositiveButton(R.string.action_done, (dialog1, which) -> {
                                solve.setComment(commentEditText.getText().toString());
                                dbHandler.updateSolve(solve);
                                Toast.makeText(getContext(), getString(R.string.added_comment), Toast.LENGTH_SHORT).show();
                                updateList();
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    break;
                case R.id.scrambleText:
                    AnimUtils.toggleContentVisibility(binding.scrambleImage);
                    break;
            }
        }
    };
    private Context mContext;

    public static TimeDialog newInstance(long id) {
        TimeDialog timeDialog = new TimeDialog();
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
        binding = DialogTimeDetailsBinding.inflate(inflater, container, false);
        //this.setEnterTransition(R.anim.activity_slide_in);
        mContext = getContext();

        mId = getArguments().getLong("id");

        //Log.d("TIME DIALOG", "mId: " + mId + "\nexists: " + handler.idExists(mId));

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //getDialog().getWindow().setWindowAnimations(R.style.DialogAnimationScale);

        final Solve matchedSolve = TwistyTimer.getDBHandler().getSolve(mId);

        if (matchedSolve != null) {
            solve = matchedSolve;

            binding.timeText.setText(Html.fromHtml(PuzzleUtils.convertTimeToString(solve.getTime(), PuzzleUtils.FORMAT_SMALL_MILLI)));
            binding.dateText.setText(new DateTime(solve.getDate()).toString("d MMM y'\n'H':'mm"));

            binding.scrambleText.setText(solve.getScramble());

            if (solve.getPenalty() == PuzzleUtils.PENALTY_DNF)
                binding.puzzlePenaltyText.setText("DNF");
            else if (solve.getPenalty() == PuzzleUtils.PENALTY_PLUSTWO)
                binding.puzzlePenaltyText.setText("+2");
            else
                binding.puzzlePenaltyText.setVisibility(View.GONE);

            if (solve.getComment() != null) {
                if (! solve.getComment().equals("")) {
                    binding.commentText.setText(solve.getComment());
                    binding.commentText.setVisibility(View.VISIBLE);
                }
            }

            if (solve.getScramble() != null) {
                if (solve.getScramble().equals(""))
                    binding.scrambleText.setVisibility(View.GONE);
            }

            binding.scrambleText.setOnClickListener(clickListener);
            binding.overflowButton.setOnClickListener(clickListener);
            binding.editButton.setOnClickListener(clickListener);
            binding.commentButton.setOnClickListener(clickListener);

        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new GenerateScrambleImage().execute();
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    private void updateList() {
        if (dialogListener != null) {
            dialogListener.onUpdateDialog();
        } else {
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        if (dialogListener != null)
            dialogListener.onDismissDialog();
        super.onDestroyView();
    }

    private class GenerateScrambleImage extends AsyncTask<Void, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Void... voids) {
            ScrambleGenerator generator = new ScrambleGenerator(solve.getPuzzle());
            return generator.generateImageFromScramble(
                    PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext()),
                    solve.getScramble());
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (binding != null && binding.scrambleImage != null)
                binding.scrambleImage.setImageDrawable(drawable);
        }
    }

}
