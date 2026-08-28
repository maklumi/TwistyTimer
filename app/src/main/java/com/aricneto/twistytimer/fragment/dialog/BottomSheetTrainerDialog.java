package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogBottomsheetRecyclerBinding;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.TrainerCursorAdapter;
import com.aricneto.twistytimer.database.AlgTaskLoader;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;

/**
 * TODO: REFACTOR
 * This code is very similar to {@link AlgListFragment}. We could generalize it to simplify
 * future changes
 */
public class BottomSheetTrainerDialog extends BottomSheetDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_SUBSET = "subset";
    private static final String KEY_CATEGORY = "category";

    private DialogBottomsheetRecyclerBinding binding;

    TrainerScrambler.TrainerSubset currentSubset;
    String                         currentCategory;
    TrainerCursorAdapter           trainerCursorAdapter;

    public BottomSheetTrainerDialog() {
    }

    // Receives broadcasts about changes to the algorithm data.
    private TTIntent.TTFragmentBroadcastReceiver mAlgDataChangedReceiver
            = new TTIntent.TTFragmentBroadcastReceiver(this, CATEGORY_ALG_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_ALGS_MODIFIED:
                    reloadList();
                    break;
            }
        }
    };
    // Receives broadcasts about changes to the time user interface.
    private TTIntent.TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTIntent.TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHANGED_CATEGORY:
                    reloadList();
                    break;
            }
        }
    };

    public static BottomSheetTrainerDialog newInstance(TrainerScrambler.TrainerSubset subset, String category) {
        BottomSheetTrainerDialog fragment = new BottomSheetTrainerDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SUBSET, subset);
        args.putString(KEY_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSubset = (TrainerScrambler.TrainerSubset) getArguments().getSerializable(KEY_SUBSET);
            currentCategory = getArguments().getString(KEY_CATEGORY);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogBottomsheetRecyclerBinding.inflate(inflater, container, false);

        binding.title.setText(R.string.trainer_spinner_title);
        Drawable icon = ThemeUtils.tintDrawable(getContext(), R.drawable.ic_outline_control_camera_24px,
                                                ContextCompat.getColor(getContext(), R.color.md_blue_A700));
        binding.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        binding.button.setVisibility(View.VISIBLE);
        binding.button.setText(R.string.trainer_select_all);
        binding.button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        binding.button.setOnClickListener(v -> {
            trainerCursorAdapter.selectAll();
            binding.list.setAdapter(trainerCursorAdapter);
            dismiss();
        });

        setupRecyclerView();
        getLoaderManager().initLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);

        registerReceiver(mAlgDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

        return binding.getRoot();
    }

    private void resetRecyclerView() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        getLoaderManager().destroyLoader(MainActivity.ALG_LIST_LOADER_ID);
    }

    public void reloadList() {
        getLoaderManager().restartLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AlgTaskLoader(currentSubset.name());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        trainerCursorAdapter.swapCursor(cursor);
        binding.list.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        trainerCursorAdapter.swapCursor(null);
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        trainerCursorAdapter = new TrainerCursorAdapter(getActivity(), null, this, currentSubset, currentCategory);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
                new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            binding.list.setLayoutManager(gridLayoutManagerVertical);
        else
            binding.list.setLayoutManager(gridLayoutManagerHorizontal);

        binding.list.setAdapter(trainerCursorAdapter);
    }
}

