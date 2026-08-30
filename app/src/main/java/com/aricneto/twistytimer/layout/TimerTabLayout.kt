package com.aricneto.twistytimer.layout

import android.content.Context
import android.util.AttributeSet
import com.aricneto.twistify.R
import com.aricneto.twistytimer.fragment.TimerFragmentMain
import com.google.android.material.tabs.TabLayout

/**
 * Wrapper class for `TabLayout` that intercepts `addTab` calls and adds an icon to
 * the tab.
 */
// NOTE: Android Support Library 23.2.0 changed the behavior when setting tabs from a view
// pager. If the view pager changes, the tabs are removed and re-created, so this is the only
// way to keep adding back the icons short of dispensing with "ViewPager" and using something
// else. See https://code.google.com/p/android/issues/detail?id=202402 for more.
class TimerTabLayout : TabLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
        when (position) {
            TimerFragmentMain.TIMER_PAGE -> tab.setIcon(R.drawable.ic_outline_timer_24px)
            TimerFragmentMain.LIST_PAGE -> tab.setIcon(R.drawable.ic_outline_list_alt_24px)
            TimerFragmentMain.GRAPH_PAGE -> tab.setIcon(R.drawable.ic_outline_timeline_24px)
        }

        super.addTab(tab, position, setSelected)
    }
}
