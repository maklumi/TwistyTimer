package com.aricneto.twistytimer.database

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SolveRepository(private val dbHandler: DatabaseHandler) {

    fun getSolves(
        puzzleType: String,
        puzzleSubtype: String,
        history: Boolean,
        comment: String,
        orderByKey: String,
        orderByDir: String
    ): Flow<List<Solve>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(fetchSolves(puzzleType, puzzleSubtype, history, comment, orderByKey, orderByDir))
            }
        }

        TTIntent.registerReceiver(receiver, TTIntent.CATEGORY_TIME_DATA_CHANGES)
        
        // Initial fetch
        trySend(fetchSolves(puzzleType, puzzleSubtype, history, comment, orderByKey, orderByDir))

        awaitClose {
            TTIntent.unregisterReceiver(receiver)
        }
    }

    private fun fetchSolves(
        puzzleType: String,
        puzzleSubtype: String,
        history: Boolean,
        comment: String,
        orderByKey: String,
        orderByDir: String
    ): List<Solve> {
        val solves = mutableListOf<Solve>()
        val cursor = dbHandler.readableDatabase.query(
            DatabaseHandler.TABLE_TIMES, null,
            (DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                    + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                    + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND "
                    + DatabaseHandler.KEY_COMMENT + " LIKE ?" + " AND "
                    + DatabaseHandler.KEY_HISTORY + "=" + (if (history) 1 else 0)),
            arrayOf(puzzleType, puzzleSubtype, "%$comment%"),
            null, null, "$orderByKey $orderByDir"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    solves.add(Solve.fromCursor(it))
                } while (it.moveToNext())
            }
        }
        return solves
    }
}
