package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.appcompat.app.AlertDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogCategorySelectBinding;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.TimerFragment;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListenerMessage;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.fragment.app.DialogFragment;

public class CategorySelectDialog extends DialogFragment {

    private static final String KEY_SAVEDSUBTYPE = "savedSubtype";

    private DialogCategorySelectBinding binding;

    // Puzzle and subtypes that are currently selected
    private String                         currentPuzzle;
    private String                         currentSubtype;
    private String                         currentTimerMode;
    private TrainerScrambler.TrainerSubset currentSubset;
    private List<String>                   subtypeList;

    // Subtype that's currently being edited
    private String currentEditSubtype = "";

    private DialogListenerMessage dialogListenerMessage;

    private BottomSheetSpinnerAdapter mAdapter;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemLongClickListener mItemLongClickListener;
    private View.OnClickListener mOnClickListener;
    private Context mContext;

    public static CategorySelectDialog newInstance(String currentPuzzle, String currentPuzzleSubtype, String currentTimerMode, TrainerScrambler.TrainerSubset currentSubset) {
        CategorySelectDialog dialog = new CategorySelectDialog();
        Bundle args = new Bundle();
        args.putString("puzzle", currentPuzzle);
        args.putString("subtype", currentPuzzleSubtype);
        args.putString("mode", currentTimerMode);
        args.putSerializable("subset", currentSubset);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCategorySelectBinding.inflate(inflater, container, false);
        mContext = getContext();

        return binding.getRoot();
    }

    @SuppressWarnings("RestrictedApi")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // retrieve arguments
        currentPuzzle = getArguments().getString("puzzle");
        currentSubtype = getArguments().getString("subtype");
        currentTimerMode = getArguments().getString("mode");
        currentSubset = (TrainerScrambler.TrainerSubset) getArguments().getSerializable("subset");

        DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // get a list of all subtypes
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);

        if (subtypeList.size() == 0) {
            // if subtype list is empty, create a new entry
            dbHandler.addSolve(new Solve(1, currentPuzzle, "Normal", 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
        } else if (subtypeList.size() == 1) {
            currentSubtype = subtypeList.get(0);
        }

        updateList(dbHandler);

        // Create subtype
        View createSubtypeView = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
        TextInputEditText createSubtypeEditText = createSubtypeView.findViewById(R.id.edit_text);
        createSubtypeEditText.setHint(R.string.enter_type_name);

        AlertDialog createSubtypeDialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.enter_type_name)
                .setView(createSubtypeView)
                .setPositiveButton(R.string.action_done, (dialog, which) -> {
                    String input = createSubtypeEditText.getText().toString();
                    if (input.length() >= 2 && input.length() <= 32) {
                        // add a single hidden solve with that category name to save it
                        dbHandler.addSolve(new Solve(1, currentPuzzle, input, 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
                        currentSubtype = input;
                        updateList(dbHandler);

                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                        editor.apply();
                        if (dialogListenerMessage != null)
                            dialogListenerMessage.onUpdateDialog(currentSubtype);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        Context context = getContext();

        // click listeners
        // select a subtype
        binding.list.setOnItemClickListener((parent, view1, position, id) -> {
            // A subtype was selected
            currentSubtype = subtypeList.get(position);

            editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
            editor.apply();
            dialogListenerMessage.onUpdateDialog(currentSubtype);
            dismiss();
        });

        binding.list.setOnItemLongClickListener((parent, view12, position, id) -> {
            // Create a popup menu for each entry
            MenuBuilder menuBuilder = new MenuBuilder(context);
            MenuInflater inflater = new MenuInflater(context);

            inflater.inflate(R.menu.menu_category_options, menuBuilder);
            MenuPopupHelper popupHelper = new MenuPopupHelper(context, menuBuilder, view12);
            popupHelper.setForceShowIcon(true);

            menuBuilder.setCallback(new MenuBuilder.Callback() {
                @Override
                public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.rename:
                            currentEditSubtype = subtypeList.get(position);
                            //Rename subtype
                            View renameView = LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null);
                            TextInputEditText renameEditText = renameView.findViewById(R.id.edit_text);
                            renameEditText.setText(currentEditSubtype);

                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.enter_new_name_dialog)
                                    .setView(renameView)
                                    .setPositiveButton(R.string.action_done, (dialog, which) -> {
                                        String input = renameEditText.getText().toString();
                                        if (input.length() >= 2 && input.length() <= 32) {
                                            dbHandler.renameSubtype(currentPuzzle, currentEditSubtype, input);
                                            currentSubtype = input;
                                            updateList(dbHandler);

                                            editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                                            if (currentTimerMode.equals(TimerFragment.TIMER_MODE_TRAINER))
                                                TrainerScrambler.renameCategory(currentSubset, currentEditSubtype, currentSubtype);
                                            editor.apply();
                                            dialogListenerMessage.onUpdateDialog(currentSubtype);
                                        }
                                    })
                                    .setNegativeButton(R.string.action_cancel, null)
                                    .show();
                            break;
                        case R.id.remove:
                            currentEditSubtype = subtypeList.get(position);
                            // Remove Subtype dialog
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.remove_subtype_confirmation)
                                    .setMessage(getString(R.string.remove_subtype_confirmation_content) + " \"" + currentEditSubtype + "\"?\n" + getString(R.string.remove_subtype_confirmation_content_continuation))
                                    .setPositiveButton(R.string.action_remove, (dialog, which) -> {
                                        dbHandler.deleteSubtype(currentPuzzle, currentEditSubtype);
                                        // After removing, change current subtype to first of list, if none exist, create "Normal" subtype
                                        if (subtypeList.size() > 1) {
                                            currentSubtype = dbHandler.getAllSubtypesFromType(currentPuzzle).get(0);
                                        } else {
                                            currentSubtype = "Normal";
                                        }
                                        updateList(dbHandler);

                                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                                        editor.apply();
                                        dialogListenerMessage.onUpdateDialog(currentSubtype);
                                    })
                                    .setNegativeButton(R.string.action_cancel, null)
                                    .show();
                            break;
                    }
                    return false;
                }

                @Override
                public void onMenuModeChange(MenuBuilder menu) {

                }
            });

            popupHelper.show();
            return true;
        });
        binding.addCategory.setOnClickListener(v -> createSubtypeDialog.show());
    }

    private void updateList(DatabaseHandler dbHandler) {
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);
        int[] icons = {};
        mAdapter = new BottomSheetSpinnerAdapter(getContext(), subtypeList.toArray(new String[0]), icons);
        binding.list.setAdapter(mAdapter);
    }

    public void setDialogListener(DialogListenerMessage dialogListener) {
        this.dialogListenerMessage = dialogListener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
