package com.aricneto.twistytimer.spans

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

/**
 * Created by philipp on 02/06/16.
 */

/**
 * Constructor that specifies to how many digits the value should be
 * formatted.
 *
 * @param digits
 */

open class RoundedAxisValueFormatter(digits: Int) : ValueFormatter() {
    /**
     * decimal format for formatting
     */
    protected var mFormat: DecimalFormat

    /**
     * Returns the number of decimal digits this formatter uses or -1, if unspecified.
     * 
     * @return
     */
    /**
     * the number of decimal digits this formatter uses
     */
    var decimalDigits: Int = 0
        protected set

    init {
        this.decimalDigits = digits

        val b = StringBuffer()
        for (i in 0..<digits) {
            if (i == 0) b.append(".")
            b.append("0")
        }

        mFormat = DecimalFormat("###,###,###,##0$b")
    }

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        // avoid memory allocations here (for performance)
        return mFormat.format(value.toInt().toLong())
    }
}
