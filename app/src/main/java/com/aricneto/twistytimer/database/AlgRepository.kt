package com.aricneto.twistytimer.database

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AlgRepository(private val dbHandler: DatabaseHandler) {

    fun getAlgorithms(subset: String): Flow<List<Algorithm>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(fetchAlgorithms(subset))
            }
        }

        TTIntent.registerReceiver(receiver, TTIntent.CATEGORY_ALG_DATA_CHANGES)
        
        // Initial fetch
        trySend(fetchAlgorithms(subset))

        awaitClose {
            TTIntent.unregisterReceiver(receiver)
        }
    }

    private fun fetchAlgorithms(subset: String): List<Algorithm> {
        val algs = mutableListOf<Algorithm>()
        val cursor = dbHandler.readableDatabase.query(
            DatabaseHandler.TABLE_ALGS, null,
            DatabaseHandler.KEY_SUBSET + "=?",
            arrayOf(subset), null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    algs.add(Algorithm.fromCursor(it))
                } while (it.moveToNext())
            }
        }
        return algs
    }
}
