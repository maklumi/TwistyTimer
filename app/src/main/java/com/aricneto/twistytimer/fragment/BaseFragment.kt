package com.aricneto.twistytimer.fragment

import android.util.TypedValue
import androidx.appcompat.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aricneto.twistytimer.activity.MainActivity

/**
 * Created by Ari on 06/06/2015.
 */
open class BaseFragment : Fragment() {
    protected val actionBarSize: Int
        get() {
            val activity: FragmentActivity = activity ?: return 0

            val typedValue = TypedValue()
            val textSizeAttr = intArrayOf(R.attr.actionBarSize)
            val indexOfAttrTextSize = 0
            val a = activity.obtainStyledAttributes(typedValue.data, textSizeAttr)
            val actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
            a.recycle()
            return actionBarSize
        }

    protected val mainActivity: MainActivity?
        get() = (activity as MainActivity?)

}
