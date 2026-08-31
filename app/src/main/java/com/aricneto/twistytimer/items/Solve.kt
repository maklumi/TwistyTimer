package com.aricneto.twistytimer.items

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Stores a solve. Solves can be converted to parcels, allowing their state to be saved and
 * restored in the context of managing the user-interface elements.
 */
@Parcelize
data class Solve(
    var id: Long = 0,
    var time: Int,
    var puzzle: String = "",
    val subtype: String = "",
    var date: Long,
    var scramble: String = "",
    var penalty: Int,
    var comment: String,
    var history: Boolean,
    var mode: Int = 0,
) : Parcelable {

    constructor(
        time: Int,
        puzzle: String = "",
        subtype: String = "",
        date: Long,
        scramble: String = "",
        penalty: Int,
        comment: String = "",
        history: Boolean,
        mode: Int = 0,
    ) : this(0, time, puzzle, subtype, date, scramble, penalty, comment, history, mode)

}
