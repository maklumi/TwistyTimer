package com.aricneto.twistytimer.items

import com.aricneto.twistytimer.utils.ThemeUtils
import androidx.annotation.StyleRes

data class Theme(
    var prefName: String = "",
    var name: String = "",
    @StyleRes var resId: Int = ThemeUtils.getThemeStyleRes(prefName),
)