package com.aricneto.twistytimer.database

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.stats.ChartStatistics
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.AlgUtils
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.PuzzleUtils

/**
 * Created by Ari on 03/06/2015.
 */
class DatabaseHandler :
    SQLiteOpenHelper(TwistyTimer.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION) {
    /**
     * An interface for notification of the progress of bulk database operations.
     */
    interface ProgressListener {
        /**
         * Notifies the listener of the progress of a bulk operation. This may be called many
         * times during the operation.
         * 
         * @param numCompleted
         * The number of sub-operations of the bulk operation that have been completed.
         * @param total
         * The total number of sub-operations that must be completed before the bulk
         * operation is complete.
         */
        fun onProgress(numCompleted: Int, total: Int)
    }

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_TIMES)
        db.execSQL(CREATE_TABLE_ALGS)
        createInitialAlgs(db)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if existed
        Log.d(
            "Database upgrade", ("Upgrading from"
                    + oldVersion.toString() + " to " + newVersion.toString())
        )
        when (oldVersion) {
            6 -> {
                db.execSQL("ALTER TABLE times ADD COLUMN $KEY_HISTORY BOOLEAN DEFAULT 0")
                edit()
                    .putInt(
                        R.string.pk_timer_text_size,
                        getInt(R.string.pk_timer_text_size, 10) * 10
                    )
                    .apply()
            }

            8 -> edit()
                .putInt(
                    R.string.pk_timer_text_size,
                    getInt(R.string.pk_timer_text_size, 10) * 10
                )
                .apply()
        }
    }

    private fun createAlg(
        db: SQLiteDatabase,
        subset: String,
        name: String,
        state: String,
        algs: String
    ) {
        val values = ContentValues()
        values.put(KEY_SUBSET, subset)
        values.put(KEY_NAME, name)
        values.put(KEY_STATE, state)
        values.put(KEY_ALGS, algs)
        values.put(KEY_PROGRESS, 0)
        db.insert(TABLE_ALGS, null, values)
    }

    /**
     * Loads an algorithm from the database for the given algorithm ID.
     * 
     * @param algID
     * The ID of the algorithm to be loaded.
     * 
     * @return
     * An [Algorithm] object created from the details loaded from the database for the
     * algorithm record matching the given ID, or `null` if no algorithm matching the
     * given ID was found.
     */
    fun getAlgorithm(algID: Long): Algorithm? {
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_ALGS,
            arrayOf(KEY_ID, KEY_SUBSET, KEY_NAME, KEY_STATE, KEY_ALGS, KEY_PROGRESS),
            "$KEY_ID=?", arrayOf(algID.toString()), null, null, null, null
        )

        cursor.use { cursor ->
            if (cursor.moveToFirst()) {
                return Algorithm(
                    cursor.getLong(0),  // id
                    cursor.getString(1) ?: "",  // subset
                    cursor.getString(2) ?: "",  // name
                    cursor.getString(3) ?: "",  // state
                    cursor.getString(4) ?: "",  // algs
                    cursor.getInt(5)
                ) // progress
            }

            // No algorithm matched the given ID.
            return null
        }
    }

    fun updateAlgorithmAlg(id: Long, alg: String): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_ALGS, alg)

        // Updating row
        return db.update(
            TABLE_ALGS, values, "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun updateAlgorithmProgress(id: Long, progress: Int): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_PROGRESS, progress)

        // Updating row
        return db.update(
            TABLE_ALGS, values, "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * Returns all solves from puzzle and category
     * @param type
     * @param subtype
     * @return
     */
    fun getAllSolvesFrom(type: String, subtype: String): Cursor {
        val db = this.readableDatabase

        val sqlSelection =
            " WHERE type =? AND subtype =? AND penalty!=" + PuzzleUtils.PENALTY_HIDETIME

        return db.rawQuery("SELECT * FROM times$sqlSelection", arrayOf(type, subtype))
    }

    /**
     * Moves all current solves from puzzle and category to history
     * 
     * @param type
     * @param subtype
     * 
     * @return
     */
    fun moveAllSolvesToHistory(type: String, subtype: String): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_HISTORY, true)

        // Updating row
        return db.update(
            TABLE_TIMES, values, "$KEY_TYPE = ? AND $KEY_SUBTYPE =?",
            arrayOf(type, subtype)
        )
    }

    /**
     * Unarchives a select number of the most recent solves
     * 
     * @param type
     * @param subtype
     * @param solves number of solves to be unarchived
     * 
     * @return
     */
    fun unarchiveSolves(type: String, subtype: String, solves: Int): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_HISTORY, false)

        // Updating row
        return db.update(
            TABLE_TIMES, values,
            KEY_ID + " IN (SELECT " + KEY_ID + " FROM " + TABLE_TIMES + " WHERE " +
                    KEY_PENALTY + " != " + PuzzleUtils.PENALTY_HIDETIME + " AND " +
                    KEY_HISTORY + " =1 AND " + KEY_TYPE + " =? AND " + KEY_SUBTYPE + " =? ORDER BY " + KEY_DATE + " DESC LIMIT ?)",
            arrayOf(type, subtype, solves.toString())
        )
    }

    /**
     * Gets the number of archived solves in the given puzzle and category
     * @param type
     * @param subtype
     * @return
     */
    fun getNumArchivedSolves(type: String, subtype: String): Long {
        val db = this.readableDatabase

        val count: Long = DatabaseUtils.queryNumEntries(db, TABLE_TIMES,
        KEY_PENALTY + " != " + PuzzleUtils.PENALTY_HIDETIME + " AND " +
                KEY_TYPE + " =? AND " + KEY_SUBTYPE + " =? AND " + KEY_HISTORY + " =1",
        arrayOf(type, subtype))
        db.close()
        return count
    }

    /**
     * Adds a new solve to the database.
     * 
     * @param solve To solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    fun addSolve(solve: Solve): Long {
        return addSolveInternal(writableDatabase, solve)
    }

    /**
     * Adds a new solve to the given database.
     * 
     * @param db    The database to which to add the solve.
     * @param solve To solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    private fun addSolveInternal(db: SQLiteDatabase, solve: Solve): Long {
        // Cutting off last digit to fix rounding errors
        var time = solve.time
        time -= (time % 10)

        val values = ContentValues()

        values.put(KEY_TYPE, solve.puzzle)
        values.put(KEY_SUBTYPE, solve.subtype)
        values.put(KEY_TIME, time)
        values.put(KEY_DATE, solve.date)
        values.put(KEY_SCRAMBLE, solve.scramble)
        values.put(KEY_PENALTY, solve.penalty)
        values.put(KEY_COMMENT, solve.comment)
        values.put(KEY_HISTORY, solve.history)

        // Inserting Row
        return db.insert(TABLE_TIMES, null, values)
    }

    /**
     * Adds a collection of new solves to the given database. The solves are added in a single
     * transaction, so this operation is much faster than adding them one-by-one using the
     * [.addSolve] method. Any given solve that matches a solve already in the
     * database will not be inserted.
     * 
     * @param fileFormat
     * The solve file format, must be [ExportImportDialog.EXIM_FORMAT_EXTERNAL], or
     * *     [ExportImportDialog.EXIM_FORMAT_BACKUP].
     * @param solves
     * The collection of solves to be added to the database. Must not be `null`, but may
     * be empty.
     * @param listener
     * An optional progress listener that will be notified as each new solve is inserted into
     * the database. Before the first new solve is added, this will be called to report that
     * zero of the total number of solves have been inserted (even if `solves` is empty).
     * Thereafter, it will be notified after each insertion. May be `null` if no progress
     * updates are required.
     * 
     * @return
     * The number of unique solves inserted. Solves that are duplicates of existing solves
     * (by [.solveExists]) are not inserted.
     */
    fun addSolves(
        fileFormat: Int,
        solves: MutableCollection<Solve>,
        listener: ProgressListener?
    ): Int {
        val total = solves.size
        var numProcessed = 0 // Whether inserted or not (i.e., includes duplicates).

        listener?.onProgress(numProcessed, total)

        var numInserted = 0 // Only those actually inserted (i.e., excludes duplicates).

        if (total > 0) {
            val db = writableDatabase

            try {
                // Wrapping the insertions in a transaction is about 50x faster!
                db.beginTransaction()

                for (solve in solves) {
                    // Do not check for duplicates if importing from external
                    if ((fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL || !solveExists(solve))) {
                        addSolveInternal(db, solve)
                        numInserted++
                    }

                    listener?.onProgress(++numProcessed, total)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        return numInserted
    }

    fun updateSolve(solve: Solve): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_TYPE, solve.puzzle)
        values.put(KEY_SUBTYPE, solve.subtype)
        values.put(KEY_TIME, solve.time)
        values.put(KEY_DATE, solve.date)
        values.put(KEY_SCRAMBLE, solve.scramble)
        values.put(KEY_PENALTY, solve.penalty)
        values.put(KEY_COMMENT, solve.comment)
        values.put(KEY_HISTORY, solve.history)

        // Updating row
        return db.update(
            TABLE_TIMES, values, "$KEY_ID = ?",
            arrayOf(solve.id.toString())
        )
    }

    /**
     * Loads a solve from the database for the given solve ID.
     * 
     * @param solveID
     * The ID of the solve to be loaded.
     * 
     * @return
     * A [Solve] object created from the details loaded from the database for the solve
     * time matching the given ID, or `null` if no solve time matching the given ID was
     * found.
     */
    fun getSolve(solveID: Long): Solve? {
        val cursor = readableDatabase.query(
            TABLE_TIMES,
            arrayOf(
                KEY_ID, KEY_TIME, KEY_TYPE, KEY_SUBTYPE, KEY_DATE, KEY_SCRAMBLE,
                KEY_PENALTY, KEY_COMMENT, KEY_HISTORY
            ),
            "$KEY_ID=?", arrayOf(solveID.toString()), null, null, null, null
        )

        cursor.use { cursor ->
            if (cursor.moveToFirst()) {
                return Solve(
                    cursor.getInt(0).toLong(),
                    cursor.getInt(1),
                    cursor.getString(2) ?: "",
                    cursor.getString(3) ?: "",
                    cursor.getLong(4),
                    cursor.getString(5) ?: "",
                    cursor.getInt(6),
                    cursor.getString(7) ?: "",
                    getBoolean(cursor, 8)
                )
            }

            // No solve matched the given ID.
            return null
        }
    }

    fun getBoolean(cursor: Cursor, columnIndex: Int): Boolean {
        return !(cursor.isNull(columnIndex) || cursor.getShort(columnIndex).toInt() == 0)
    }

    fun getAllSubtypesFromType(type: String): MutableList<String> {
        val subtypesList: MutableList<String> = ArrayList()

        val db = this.readableDatabase

        val cursor = db.rawQuery(
            ("SELECT DISTINCT " + KEY_SUBTYPE + " FROM "
                    + TABLE_TIMES + " WHERE " + KEY_TYPE + " ='" + type + "' ORDER BY " + KEY_SUBTYPE + " ASC"),
            null
        )

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(KEY_SUBTYPE)
            do {
                subtypesList.add(cursor.getString(columnIndex) ?: "")
            } while (cursor.moveToNext())
        }

        cursor.close()
        return subtypesList
    }

    /**
     * Populates the collection of statistics (average calculators) with the solve times recorded
     * in the database. The statistics will manage the segregation of solves for the current session
     * only from those from all past and current sessions. If all average calculators are for the
     * current session only, only the times for the current session will be read from the database.
     * 
     * @param puzzleType
     * The name of the puzzle type.
     * @param puzzleSubtype
     * The name of the puzzle subtype.
     * @param statistics
     * The statistics in which to record the solve times. This may contain any mix of average
     * calculators for all sessions or only the current session. The database read will be
     * adapted automatically to read the minimum number of rows to satisfy the collection of
     * the required statistics.
     */
    fun populateStatistics(
        puzzleType: String, puzzleSubtype: String, statistics: Statistics
    ) {
        val isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly

        // Sort into ascending order of date (oldest solves first), so that the "current"
        // average is, in the end, calculated to be that of the most recent solves.
        val sql: String = if (isStatisticsForCurrentSessionOnly) {
            ("SELECT " + KEY_TIME + ", " + KEY_PENALTY + " FROM " + TABLE_TIMES
                    + " WHERE " + KEY_TYPE + "=? AND " + KEY_SUBTYPE + "=? AND "
                    + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                    + KEY_HISTORY + "=0 ORDER BY " + KEY_DATE + " ASC")
        } else {
            ("SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_HISTORY
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " ORDER BY " + KEY_DATE + " ASC")
        }

        val cursor =
            readableDatabase.rawQuery(sql, arrayOf(puzzleType, puzzleSubtype))

        cursor.use { cursor ->
            val timeCol = cursor.getColumnIndex(KEY_TIME)
            val penaltyCol = cursor.getColumnIndex(KEY_PENALTY)
            val historyCol =
                if (isStatisticsForCurrentSessionOnly) -1 else cursor.getColumnIndex(KEY_HISTORY)

            while (cursor.moveToNext()) {
                val isForCurrentSession =
                    isStatisticsForCurrentSessionOnly || cursor.getInt(historyCol) == 0

                if (cursor.getInt(penaltyCol) == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(isForCurrentSession)
                } else {
                    statistics.addTime(cursor.getLong(timeCol), isForCurrentSession)
                }
            }
        }
    }

    /**
     * Populates the chart statistics with the solve times recorded in the database. If all
     * statistics are for the current session only, only the times for the current session will be
     * read from the database.
     * 
     * @param puzzleType
     * The name of the puzzle type.
     * @param puzzleSubtype
     * The name of the puzzle subtype.
     * @param statistics
     * The chart statistics in which to record the solve times. This may require solve times for
     * all sessions or only the current session. The database read will be adapted automatically
     * to read the minimum number of rows to satisfy the collection of the required statistics.
     */
    fun populateChartStatistics(
        puzzleType: String, puzzleSubtype: String, statistics: ChartStatistics
    ) {
        val isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly

        // Sort into ascending order of date (oldest solves first), so that the "current"
        // average is, in the end, calculated to be that of the most recent solves.
        val sql: String = if (isStatisticsForCurrentSessionOnly) {
            ("SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_DATE
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " AND " + KEY_HISTORY + "=0 ORDER BY " + KEY_DATE + " ASC")
        } else {
            // NOTE: A change from the old approach: the "all time" option include those from the
            // current session, too. This is consistent with the way "all time statistics" are
            // calculated for the table of statistics.
            ("SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_DATE
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " ORDER BY " + KEY_DATE + " ASC")
        }

        val cursor =
            readableDatabase.rawQuery(sql, arrayOf(puzzleType, puzzleSubtype))

        cursor.use { cursor ->
            val timeCol = cursor.getColumnIndex(KEY_TIME)
            val penaltyCol = cursor.getColumnIndex(KEY_PENALTY)
            val dateCol = cursor.getColumnIndex(KEY_DATE)

            while (cursor.moveToNext()) {
                if (cursor.getInt(penaltyCol) == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(cursor.getLong(dateCol))
                } else {
                    statistics.addTime(cursor.getLong(timeCol), cursor.getLong(dateCol))
                }
            }
        }
    }

    /**
     * Deletes a single solve matching the given ID from the database.
     * 
     * @param solveID
     * The ID of the solve record in the "times" table of the database.
     * 
     * @return
     * The number of records deleted. If no record matches `solveID`, the result is zero.
     */
    fun deleteSolveByID(solveID: Long): Int {
        return deleteSolveByIDInternal(writableDatabase, solveID)
    }

    /**
     * Deletes a single solve from the database.
     * 
     * @param solve
     * To solve to be deleted. The corresponding database record to be deleted from the "times"
     * table is matched using the ID returned from [Solve.id].
     * 
     * @return
     * The number of records deleted. If no record matches the ID of the solve, the result is
     * zero.
     */
    fun deleteSolve(solve: Solve): Int {
        return deleteSolveByIDInternal(writableDatabase, solve.id)
    }

    /**
     * Deletes multiple solves from the database that match the solve record IDs in the given
     * collection. The solves are deleted in the context of a single database transaction.
     * 
     * @param solveIDs
     * The IDs of the solve records in the "times" table of the database to be deleted. Must
     * not be `null`, but may be empty.
     * @param listener
     * An optional progress listener that will be notified as each solve is deleted from the
     * database. Before the first solve is deleted, this will be called to report that zero of
     * the total number of solves have been deleted (even if `solveIDs` is empty).
     * Thereafter, it will be notified after each attempted deletion by ID, whether a matching
     * solve was found or not. May be `null` if no progress reports are required.
     * 
     * @return
     * The number of records deleted. If an ID from `solveIDs` does not match any record,
     * or if an ID is a duplicate of an ID that has already been deleted, that ID is ignored.
     * So the result may be less than the number of solve IDs in the collection.
     */
    fun deleteSolvesByID(solveIDs: MutableCollection<Long>, listener: ProgressListener?): Int {
        val total = solveIDs.size
        var numProcessed = 0 // Whether deleted or not (i.e., includes RNF and duplicates).

        listener?.onProgress(numProcessed, total)

        var numDeleted = 0 // Only those actually deleted (i.e., excludes RNF and duplicates).

        if (total > 0) {
            val db = writableDatabase

            try {
                // Wrap the bulk delete operations in a transaction; it is *much* faster,
                db.beginTransaction()

                for (id in solveIDs) {
                    // May not change if RNF or if ID is a duplicate and is already deleted.
                    numDeleted += deleteSolveByIDInternal(db, id)

                    listener?.onProgress(++numProcessed, total)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        return numDeleted
    }

    /**
     * Deletes a single solve matching the given ID from the database.
     * 
     * @param db
     * The database from which to delete the solve.
     * @param solveID
     * The ID of the solve record in the "times" table of the database.
     * 
     * @return
     * The number of records deleted. If no record matches `solveID`, the result is zero.
     */
    private fun deleteSolveByIDInternal(db: SQLiteDatabase, solveID: Long): Int {
        return db.delete(TABLE_TIMES, "$KEY_ID=?", arrayOf(solveID.toString()))
    }

    // Delete entries from session
    fun deleteAllFromSession(type: String, subtype: String): Int {
        val db = this.writableDatabase
        return db.delete(
            TABLE_TIMES,
            "$KEY_TYPE=? AND $KEY_SUBTYPE = ? AND $KEY_HISTORY=0",
            arrayOf(type, subtype)
        )
    }

    /**
     * Deletes all solves from a subtype, thus removing the subtype
     * 
     * @param subtype
     */
    fun deleteSubtype(type: String, subtype: String): Int {
        val db = this.writableDatabase
        return db.delete(
            TABLE_TIMES, "$KEY_TYPE=? AND $KEY_SUBTYPE = ?",
            arrayOf(type, subtype)
        )
    }

    /**
     * Renames a subtype
     * 
     * @param subtype
     */
    fun renameSubtype(type: String, subtype: String, newName: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_SUBTYPE, newName)
        return db.update(
            TABLE_TIMES,
            contentValues,
            "$KEY_TYPE=? AND $KEY_SUBTYPE=?",
            arrayOf(type, subtype)
        )
    }

    val allSolves: Cursor
        get() {
            val db = this.readableDatabase
            return db.rawQuery(
                "SELECT * FROM times WHERE penalty!=" + PuzzleUtils.PENALTY_HIDETIME,
                null
            )
        }

    fun solveExists(solve: Solve): Boolean {
        val db = this.readableDatabase

        return DatabaseUtils.queryNumEntries(
            db,
            TABLE_TIMES,
            "type=? AND subtype =? AND time=? AND scramble=? AND date=?",
            arrayOf(
                solve.puzzle,
                solve.subtype,
                solve.time.toString(),
                solve.scramble,
                solve.date.toString()
            )
        ) > 0
    }

    // this info should REALLY be in a separate file. I'll get to it when I add other alg sets.
    private fun createInitialAlgs(db: SQLiteDatabase) {
        createAlg(
            db, SUBSET_OLL, "OLL 01", "NNNNYNNNNNYNYYYNYNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 01"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 02", "NNNNYNNNNNYYNYNYYNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 02"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 03", "NNNNYNYNNYYNYYNYYNNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 03"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 04", "NNNNYNNNYNYYNYNNYYNYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 04"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 05", "NNNNYYNYYYYNYNNNNNYYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 05"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 06", "NYYNYYNNNNNNNNYNYYNYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 06"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 07", "NYNYYNYNNYNNYYNYYNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 07"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 08", "NYNNYYNNYNNYNNNNYYNYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 08"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 09", "NNYYYNNYNNYNNYYNNYNNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 09"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 10", "NNYYYNNYNYYNNYNYNNYNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 10"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 11", "NNNNYYYYNYYNYNNYNNNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 11"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 12", "NNYNYYNYNNYNNNYNNYNYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 12"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 13", "NNNYYYYNNYYNYNNYYNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 13"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 14", "NNNYYYNNYNYYNNNNYYNNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 14"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 15", "NNNYYYNNYYYNYNNNYNYNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 15"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 16", "NNYYYYNNNNYNNNYNYYNNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 16"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 17", "YNNNYNNNYNYYNYNNYNYYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 17"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 18", "YNYNYNNNNNYNNYNYYYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 18"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 19", "YNYNYNNNNNYNNYYNYNYYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 19"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 20", "YNYNYNYNYNYNNYNNYNNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 20"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 21", "NYNYYYNYNNNNYNYNNNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 21"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 22", "NYNYYYNYNNNYNNNYNNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 22"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 23", "YYYYYYNYNNNNNNNYNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 23"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 24", "NYYYYYNYYYNNNNNNNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 24"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 25", "YYNYYYNYYNNNYNNNNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 25"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 26", "YYNYYYNYNNNYNNYNNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 26"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 27", "NYNYYYYYNYNNYNNYNNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 27"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 28", "YYYYYNYNYNNNNYNNYNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 28"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 29", "YNYYYNNYNNYNNYYNNNYNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 29"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 30", "YNYNYYNYNNYNNNYNNNYYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 30"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 31", "NYYNYYNNYYNNNNNNYYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 31"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 32", "NNYNYYNYYYYNNNNNNYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 32"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 33", "NNYYYYNNYYYNNNNNYYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 33"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 34", "YNYYYYNNNNYNNNYNYNYNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 34"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 35", "YNNNYYNYYNYNYNNNNYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 35"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 36", "YNNYYNNYYNYNYYNNNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 36"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 37", "YYNYYNNNYNNNYYNNYYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 37"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 38", "NYYYYNYNNYNNNYYNYNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 38"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 39", "YYNNYNNYYNNYNYNNNNYYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 39"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 40", "NYYNYNYYNNNNNYNYNNNYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 40"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 41", "YNYNYYNYNNYNNNNYNYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 41"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 42", "YNYYYNNYNNYNNYNYNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 42"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 43", "YNNYYNYYNNYNYYYNNNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 43"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 44", "NNYNYYNYYNYNNNNNNNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 44"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 45", "NNYYYYNNYNYNNNNNYNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 45"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 46", "YYNNYNYYNNNNYYYNNNNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 46"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 47", "NYNNYYNNNYNNYNYNYYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 47"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 48", "NYNYYNNNNNNYNYNYYNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 48"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 49", "NNNYYNNYNYYNYYYNNYNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 49"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 50", "NNNNYYNYNNYYNNNYNNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 50"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 51", "NNNYYYNNNNYYNNNYYNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 51"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 52", "NYNNYNNYNYNNYYYNNYNYN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 52"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 53", "NNNNYYNYNNYNYNYNNNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 53"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 54", "NYNNYYNNNNNNYNYNYNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 54"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 55", "NYNNYNNYNNNNYYYNNNYYY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 55"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 56", "NNNYYYNNNNYNYNYNYNYNY", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 56"
            )
        )
        createAlg(
            db, SUBSET_OLL, "OLL 57", "YNYYYYYNYNYNNNNNYNNNN", AlgUtils.getDefaultAlgs(
                SUBSET_OLL, "OLL 57"
            )
        )

        // PLL
        createAlg(
            db,
            SUBSET_PLL,
            "H",
            "YYYYYYYYYOROGBGRORBGB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "H")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ua",
            "YYYYYYYYYOBOGOGRRRBGB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ua")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ub",
            "YYYYYYYYYOGOGBGRRRBOB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ub")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Z",
            "YYYYYYYYYOBOGRGRGRBOB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Z")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Aa",
            "YYYYYYYYYGOGRGBORRBBO",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Aa")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ab",
            "YYYYYYYYYOORBGOGRGRBB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ab")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "E",
            "YYYYYYYYYGOBOGRBRGRBO",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "E")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "F",
            "YYYYYYYYYGOBOBGRRRBGO",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "F")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ga",
            "YYYYYYYYYRBOGGRBOBORG",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ga")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Gb",
            "YYYYYYYYYBROGGBOBGROR",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gb")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Gc",
            "YYYYYYYYYOGRBROGOGRBB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gc")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Gd",
            "YYYYYYYYYORGRORBGOGBB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gd")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ja",
            "YYYYYYYYYBOOGGGRBBORR",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ja")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Jb",
            "YYYYYYYYYOOGRROGGRBBB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Jb")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Na",
            "YYYYYYYYYOORBBGRROGGB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Na")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Nb",
            "YYYYYYYYYROOGBBORRBGG",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Nb")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Ra",
            "YYYYYYYYYOGOGORBRGRBB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ra")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Rb",
            "YYYYYYYYYGOBORGRGRBBO",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Rb")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "T",
            "YYYYYYYYYOOGRBOGRRBGB",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "T")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "V",
            "YYYYYYYYYRGOGOBORRBBG",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "V")
        )
        createAlg(
            db,
            SUBSET_PLL,
            "Y",
            "YYYYYYYYYRBOGGBORRBOG",
            AlgUtils.getDefaultAlgs(SUBSET_PLL, "Y")
        )
    }

    companion object {
        const val TABLE_TIMES: String = "times"

        // Times table
        const val KEY_ID: String = "_id"
        const val KEY_TYPE: String = "type"
        const val KEY_SUBTYPE: String = "subtype"
        const val KEY_TIME: String = "time"
        const val KEY_DATE: String = "date"
        const val KEY_SCRAMBLE: String = "scramble"
        const val KEY_PENALTY: String = "penalty"
        const val KEY_COMMENT: String = "comment"
        const val KEY_HISTORY: String = "history"

        const val IDX_TYPE: Int = 1
        const val IDX_SUBTYPE: Int = 2
        const val IDX_TIME: Int = 3
        const val IDX_DATE: Int = 4
        const val IDX_SCRAMBLE: Int = 5
        const val IDX_PENALTY: Int = 6
        const val IDX_COMMENT: Int = 7

        // Algs table
        const val TABLE_ALGS: String = "algs"
        const val KEY_SUBSET: String = "subset"
        const val KEY_NAME: String = "name"
        const val KEY_STATE: String = "state"
        const val KEY_ALGS: String = "algs"
        const val KEY_PROGRESS: String = "progress"

        const val SUBSET_OLL: String = "OLL"
        const val SUBSET_PLL: String = "PLL"

        // Database Version
        private const val DATABASE_VERSION = 10

        // Database Name
        private const val DATABASE_NAME = "databaseManager"
        private const val CREATE_TABLE_TIMES = ("CREATE TABLE " + TABLE_TIMES + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TYPE + " TEXT,"
                + KEY_SUBTYPE + " TEXT,"
                + KEY_TIME + " INTEGER,"
                + KEY_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
                + KEY_SCRAMBLE + " TEXT,"
                + KEY_PENALTY + " INTEGER,"
                + KEY_COMMENT + " TEXT,"
                + KEY_HISTORY + " BOOLEAN"
                + ")")
        private const val CREATE_TABLE_ALGS = ("CREATE TABLE " + TABLE_ALGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_SUBSET + " TEXT,"
                + KEY_NAME + " TEXT,"
                + KEY_STATE + " TEXT,"
                + KEY_ALGS + " TEXT,"
                + KEY_PROGRESS + " INTEGER"
                + ")")

        const val DIR_DESC: String = "DESC"
        const val DIR_ASC: String = "ASC"
    }
}
