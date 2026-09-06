package com.aricneto.twistytimer.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Stat
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor

/**
 * Created by Ari Neto on 19-Aug-17.
 * 
 * An adapter that's used to fill the Statistics card in [com.aricneto.twistytimer.fragment.TimerGraphFragment]
 */
class StatGridAdapter(
    private val mContext: Context,
    private val mStats: ArrayList<Stat>
) : BaseAdapter() {

    override fun getCount(): Int {
        return mStats.size
    }

    override fun getItem(index: Int): Any {
        return mStats[index]
    }

    override fun getItemId(i: Int): Long {
        return mStats[i].row.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView = TextView(mContext, null, R.attr.statTextStyle)

        // alternate colors between rows to make viewing easier
        if (getItemId(position) % 2 == 0L) textView.setBackgroundColor(
            fetchAttrColor(
                mContext,
                R.attr.colorSurfaceVariant
            )
        )
        else textView.setBackgroundColor(
            fetchAttrColor(
                mContext,
                R.attr.colorSurfaceContainer
            )
        )

        textView.setTypeface(ResourcesCompat.getFont(mContext, R.font.quicksand))
        textView.text = mStats[position].time

        return textView
    }
}
