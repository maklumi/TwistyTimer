package com.aricneto.twistytimer.fragment

import android.app.Activity
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.View
import androidx.appcompat.R
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.utils.ThemeUtils

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

    /**
     * This function should be called in every fragment that needs a toolbar
     * Every fragment has its own toolbar, and this function executes the
     * necessary steps to ensure the toolbar is correctly bound to the main
     * activity, which handles the rest (drawer and options menu)
     * 
     * 
     * Also, a warning: always bind the toolbar title BEFORE calling this function
     * otherwise, it won't work.
     * 
     * @param toolbar The toolbar present in the fragment
     */
    protected fun setupToolbarForFragment(toolbar: Toolbar) {
        toolbar.setNavigationIcon(com.aricneto.twistify.R.drawable.ic_outline_settings_24px)
        toolbar.navigationIcon!!.setColorFilter(
            ThemeUtils.fetchAttrColor(
                requireContext(),
                com.aricneto.twistify.R.attr.colorTimerText
            ), PorterDuff.Mode.SRC_IN
        )

        this.mainActivity!!.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { this@BaseFragment.mainActivity!!.openDrawer() }
    }

    protected val mainActivity: MainActivity?
        get() = (activity as MainActivity?)

    protected val mainFragmentManager: FragmentManager?
        get() = requireActivity().getFragmentManager() as FragmentManager?

    protected val screenHeight: Int
        get() {
            val activity: FragmentActivity = activity ?: return 0
            return activity.findViewById<View>(android.R.id.content).getHeight()
        }
}
