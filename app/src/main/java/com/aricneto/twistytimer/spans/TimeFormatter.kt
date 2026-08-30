package com.aricneto.twistytimer.spans

import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Created by Ari on 06/02/2016.
 */
class TimeFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return convertTimeToString((value * 1000L).toLong(), PuzzleUtils.FORMAT_NO_MILLI)
    }
}
