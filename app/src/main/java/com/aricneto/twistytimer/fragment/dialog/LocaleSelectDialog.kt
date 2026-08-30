package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.DialogSettingsChangeLocaleBinding;
import com.aricneto.twistytimer.activity.SettingsActivity;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.LocaleUtils;

import java.util.LinkedHashMap;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Dialog used to select application language
 */

public class LocaleSelectDialog extends DialogFragment implements DialogListener{

    private DialogSettingsChangeLocaleBinding binding;

    public static LocaleSelectDialog newInstance() {
        return new LocaleSelectDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogSettingsChangeLocaleBinding.inflate(inflater, container, false);

        binding.recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerView.setAdapter(new LocaleSelectAdapter(getActivity(), this));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onUpdateDialog() {

    }

    @Override
    public void onDismissDialog() {
        dismiss();
    }
}

class LocaleSelectAdapter extends RecyclerView.Adapter<LocaleSelectAdapter.CardViewHolder> {

    private FragmentActivity                              mActivity;
    private String                                        oldLocale;
    private String                                        newLocale;
    private LinkedHashMap<String, Pair<Integer, Integer>> localeHash;
    private String[]                                      locales;
    private DialogListener                                dialogListener;

    LocaleSelectAdapter(FragmentActivity mActivity, DialogListener listener) {
        this.mActivity = mActivity;
        this.oldLocale = LocaleUtils.getLocale();
        this.localeHash = LocaleUtils.getLocaleHashMap();
        this.locales = LocaleUtils.getLocaleArray();
        this.dialogListener = listener;
    }

    @Override
    public LocaleSelectAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_locale, parent, false);

        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        String itemLocale = locales[position];
        holder.localeItem.setText(localeHash.get(itemLocale).first);
        holder.localeItem.setCompoundDrawablesWithIntrinsicBounds(localeHash.get(itemLocale).second, 0, 0, 0);

        holder.localeItem.setOnClickListener(v -> {
            newLocale = itemLocale;
            // If the locale has been changed, then the activity will need to be recreated. The
            // locale can only be applied properly during the inflation of the layouts, so it has
            // to go back to "Activity.updateLocale()" to do that.
            if (!newLocale.equals(oldLocale)) {
                LocaleUtils.setLocale(newLocale);
                ((SettingsActivity) mActivity).onRecreateRequired();
                dialogListener.onDismissDialog();
            }
        });
    }

    @Override
    public int getItemCount() {
        return locales.length;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView localeItem;

        public CardViewHolder(View view) {
            super(view);
            this.localeItem = view.findViewById(R.id.locale_item);
        }
    }
}
