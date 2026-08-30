package com.aricneto.twistytimer.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.aricneto.twistify.R

/**
 * Custom adapter for [com.aricneto.twistytimer.fragment.dialog.BottomSheetSpinnerDialog]
 * Populates ListView with simple text-icon pairs.
 */
class BottomSheetSpinnerAdapter(
    private val mContext: Context,
    private val mTitles: Array<String?>,
    private val mIconRes: IntArray
) : BaseAdapter() {

    private val iconResLength: Int = mIconRes.size

    // ViewHolder holds references to views
    private class ViewHolder(val titleView: TextView)

    override fun getCount(): Int = mTitles.size

    override fun getItem(position: Int): String? = mTitles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_bottom_spinner, parent, false)
            holder = ViewHolder(view.findViewById(R.id.item))
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val titleView = holder.titleView
        titleView.text = mTitles[position]

        if (iconResLength > 0 && mIconRes[position] != 0) {
            try {
                val icon = VectorDrawableCompat.create(mContext.resources, mIconRes[position], null)
                titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            } catch (e: Exception) {
                Log.e("BottomSheetSpinner", "Error populating list!: $e")
            }
        }

        return view
    }
}

/*

        val view =
            LayoutInflater.from(mContext).inflate(R.layout.item_bottom_spinner, parent, false)

        val titleView = view.findViewById<TextView>(R.id.item)
        val icon: Drawable?

        titleView.text = mTitles[position]

        if (iconResLength > 0) {
            if (mIconRes[position] != 0) {
                try {
                    icon = VectorDrawableCompat.create(
                        mContext.resources,
                        mIconRes[position],
                        null
                    )
                    titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                } catch (e: Exception) {
                    Log.e("BottomSheetSpinner", "Error populating list!: $e")
                }
            }
        } else {
            icon = VectorDrawableCompat.create(mContext.resources, R.drawable.ic_label, null)
            icon?.setAlpha(90)
            titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        }

        return view

*/