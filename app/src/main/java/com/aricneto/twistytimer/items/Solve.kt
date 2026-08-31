package com.aricneto.twistytimer.items

import android.database.Cursor
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

    companion object {
        fun fromCursor(cursor: Cursor): Solve {
            return Solve(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                time = cursor.getInt(cursor.getColumnIndexOrThrow("time")),
                puzzle = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: "",
                subtype = cursor.getString(cursor.getColumnIndexOrThrow("subtype")) ?: "",
                date = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
                scramble = cursor.getString(cursor.getColumnIndexOrThrow("scramble")) ?: "",
                penalty = cursor.getInt(cursor.getColumnIndexOrThrow("penalty")),
                comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")) ?: "",
                history = cursor.getInt(cursor.getColumnIndexOrThrow("history")) == 1,
                mode = cursor.getInt(cursor.getColumnIndexOrThrow("mode"))
            )
        }
    }
}
