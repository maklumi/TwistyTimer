package com.aricneto.twistytimer.fragment.dialog

import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogSettingsChangeLocaleBinding
import com.aricneto.twistytimer.activity.SettingsActivity
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.LocaleUtils.locale
import com.aricneto.twistytimer.utils.LocaleUtils.localeArray
import com.aricneto.twistytimer.utils.LocaleUtils.localeHashMap
import androidx.core.graphics.drawable.toDrawable

/**
 * Dialog used to select application language
 */
class LocaleSelectDialog : DialogFragment(), DialogListener {
    private var binding: DialogSettingsChangeLocaleBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSettingsChangeLocaleBinding.inflate(inflater, container, false)

        binding!!.recyclerView.setLayoutManager(GridLayoutManager(context, 2))
        binding!!.recyclerView.setAdapter(LocaleSelectAdapter(requireActivity(), this))

        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onUpdateDialog() {
    }

    override fun onDismissDialog() {
        dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(): LocaleSelectDialog {
            return LocaleSelectDialog()
        }
    }
}

internal class LocaleSelectAdapter(
    private val mActivity: FragmentActivity,
    private val dialogListener: DialogListener
) : RecyclerView.Adapter<LocaleSelectAdapter.CardViewHolder?>() {
    private val oldLocale: String? = locale
    private var newLocale: String? = null
    private val localeHash: LinkedHashMap<String, Pair<Int, Int>> = localeHashMap
    private val locales: Array<String> = localeArray

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        // create a new view
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_locale, parent, false)

        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val itemLocale = locales[position]
        holder.localeItem.setText(localeHash[itemLocale]!!.first!!)
        holder.localeItem.setCompoundDrawablesWithIntrinsicBounds(
            localeHash[itemLocale]!!.second!!,
            0,
            0,
            0
        )

        holder.localeItem.setOnClickListener { _: View? ->
            newLocale = itemLocale
            // If the locale has been changed, then the activity will need to be recreated. The
            // locale can only be applied properly during the inflation of the layouts, so it has
            // to go back to "Activity.updateLocale()" to do that.
            if (newLocale != oldLocale) {
                locale = newLocale
                (mActivity as SettingsActivity).onRecreateRequired()
                dialogListener.onDismissDialog()
            }
        }
    }

    override fun getItemCount(): Int {
        return locales.size
    }

    internal class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var localeItem: TextView = view.findViewById(R.id.locale_item)
    }
}
