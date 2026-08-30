package com.aricneto.twistytimer.items

import android.database.Cursor
import com.aricneto.twistytimer.database.DatabaseHandler

/**
 * Stores an algorithm for use in the alg list
 */
data class Algorithm(
    val id: Long,
    var subset: String = "",
    var name: String = "",
    var state: String = "",
    var algs: String = "",
    var progress: Int
) {
    companion object {
        fun fromCursor(cursor: Cursor): Algorithm {
            return Algorithm(
                id = cursor.getLong(0),
                subset = cursor.getString(1),
                name = cursor.getString(2),
                state = cursor.getString(3),
                algs = cursor.getString(4),
                progress = cursor.getInt(5)
            )
        }
    }
}
