package com.aricneto.twistytimer.database

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.fragment.TimerFragment
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.stats.ChartStatistics
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.AlgUtils
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getInt
import com.aricneto.twistytimer.utils.PuzzleUtils
import kotlinx.coroutines.runBlocking

/**
 * Created by Ari on 03/06/2015.
 */
class DatabaseHandler :
    SQLiteOpenHelper(TwistyTimer.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION) {

    private fun getSolveQueries() = TwistyDatabaseFactory.getDatabase().solveQueries
    private fun getAlgorithmQueries() = TwistyDatabaseFactory.getDatabase().algorithmQueries

    /**
     * An interface for notification of the progress of bulk database operations.
     */
    interface ProgressListener {
        /**
         * Notifies the listener of the progress of a bulk operation. This may be called many
         * times during a single operation.
         * 
         * @param count The number of records processed so far.
         * @param total The total number of records to be processed.
         */
        fun onProgress(count: Int, total: Int)
    }

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        // SQLDelight handles table creation if not exists
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // SQLDelight handles schema management via its driver
    }

    private fun createAlg(
        db: SQLiteDatabase,
        subset: String,
        name: String,
        state: String,
        algs: String
    ) {
        getAlgorithmQueries().insertAlg(subset, name, state, algs, 0)
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
        return getAlgorithmQueries().selectById(algID).executeAsOneOrNull()?.let { sqAlg ->
            Algorithm(
                sqAlg._id,
                sqAlg.subset ?: "",
                sqAlg.name ?: "",
                sqAlg.state ?: "",
                sqAlg.algs ?: "",
                (sqAlg.progress ?: 0).toInt()
            )
        }
    }

    fun updateAlgorithmAlg(id: Long, alg: String): Int {
        getAlgorithmQueries().updateAlg(alg, id)
        return 1
    }

    fun updateAlgorithmProgress(id: Long, progress: Int): Int {
        getAlgorithmQueries().updateProgress(progress.toLong(), id)
        return 1
    }

    /**
     * Returns all solves from puzzle and category
     * @param type
     * @param subtype
     * @return
     */
    fun getAllSolvesFrom(type: String, subtype: String, mode: Int): Cursor {
        return readableDatabase.query(
            TABLE_TIMES, null, "$KEY_TYPE = ? AND $KEY_SUBTYPE = ? AND mode = ? AND ($KEY_HISTORY = 0 OR $KEY_HISTORY IS NULL)",
            arrayOf(type, subtype, mode.toString()), null, null, "$KEY_DATE DESC"
        )
    }

    /**
     * Moves all current solves from puzzle and category to history
     * 
     * @param type
     * @param subtype
     * 
     * @return
     */
    fun moveAllSolvesToHistory(type: String, subtype: String, mode: Int): Int {
        getSolveQueries().moveAllSolvesToHistory(type, subtype, mode)
        return 1
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
    fun unarchiveSolves(type: String, subtype: String, mode: Int, solves: Int): Int {
        getSolveQueries().unarchiveSolves(type, subtype, mode, solves.toLong())
        return 1
    }

    /**
     * Returns how many archived solves are in this session
     * 
     * @param type
     * @param subtype
     * 
     * @return
     */
    fun getNumArchivedSolves(type: String, subtype: String, mode: Int): Long {
        return getSolveQueries().getNumArchivedSolves(type, subtype, mode).executeAsOne()
    }

    /**
     * Adds a new solve to the database.
     * 
     * @param solve To solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    fun addSolve(solve: Solve): Long {
        return getSolveQueries().transactionWithResult {
            getSolveQueries().insertSolve(
                solve.puzzle, solve.subtype, solve.time.toLong(), solve.date,
                solve.scramble, solve.penalty.toLong(), solve.comment, solve.history, solve.mode
            )
            getSolveQueries().lastInsertId().executeAsOne()
        }
    }

    /**
     * Adds a new solve to the given database.
     * 
     * @param db    The database to which to add the solve.
     * @param solve To solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    private fun addSolveInternal(db: SQLiteDatabase, solve: Solve): Long {
        return addSolve(solve)
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
            getSolveQueries().transaction {
                for (solve in solves) {
                    // Do not check for duplicates if importing from external
                    if ((fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL || !solveExists(solve))) {
                        getSolveQueries().insertSolve(
                            solve.puzzle, solve.subtype, solve.time.toLong(), solve.date,
                            solve.scramble, solve.penalty.toLong(), solve.comment, solve.history, solve.mode
                        )
                        numInserted++
                    }

                    listener?.onProgress(++numProcessed, total)
                }
            }
        }

        return numInserted
    }

    fun updateSolve(solve: Solve): Int {
        getSolveQueries().updateSolve(
            solve.time.toLong(), solve.date, solve.scramble,
            solve.penalty.toLong(), solve.comment, solve.history, solve.mode, solve.id
        )
        return 1
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
        return getSolveQueries().selectById(solveID).executeAsOneOrNull()?.let { sqSolve ->
            Solve(
                sqSolve._id,
                (sqSolve.time ?: 0).toInt(),
                sqSolve.type ?: "",
                sqSolve.subtype ?: "",
                sqSolve.date,
                sqSolve.scramble ?: "",
                (sqSolve.penalty ?: 0).toInt(),
                sqSolve.comment ?: "",
                sqSolve.history ?: false,
                sqSolve.mode
            )
        }
    }

    fun getBoolean(cursor: Cursor, columnIndex: Int): Boolean {
        return !(cursor.isNull(columnIndex) || cursor.getShort(columnIndex).toInt() == 0)
    }

    fun getAllSubtypesFromType(type: String, mode: Int): MutableList<String> {
        return getSolveQueries().getAllSubtypesFromType(type, mode).executeAsList().map { it.subtype ?: "" }.toMutableList()
    }

    /**
     * Populates the collection of statistics (average calculators) with the solve times recorded
     * in the database.
     */
    fun populateStatistics(
        puzzleType: String, puzzleSubtype: String, mode: Int, statistics: Statistics
    ) {
        runBlocking {
            TwistyTimer.getSolveRepository().populateStatistics(puzzleType, puzzleSubtype, mode, statistics)
        }
    }

    /**
     * Populates the chart statistics with the solve times recorded in the database.
     */
    fun populateChartStatistics(
        puzzleType: String, puzzleSubtype: String, mode: Int, statistics: ChartStatistics
    ) {
        runBlocking {
            TwistyTimer.getSolveRepository().populateChartStatistics(puzzleType, puzzleSubtype, mode, statistics)
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
        getSolveQueries().deleteSolve(solveID)
        return 1
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
        return deleteSolveByID(solve.id)
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
        var numProcessed = 0

        listener?.onProgress(numProcessed, total)

        var numDeleted = 0

        if (total > 0) {
            getSolveQueries().transaction {
                for (id in solveIDs) {
                    getSolveQueries().deleteSolve(id)
                    numDeleted++
                    listener?.onProgress(++numProcessed, total)
                }
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
        return deleteSolveByID(solveID)
    }

    /**
     * Deletes all solves for a given puzzle and category.
     * 
     * @param type    The name of the puzzle type.
     * @param subtype The name of the puzzle subtype.
     * @return The number of rows deleted.
     */
    fun deleteAllFromSession(type: String, subtype: String, mode: Int): Int {
        getSolveQueries().deleteAllFromSession(type, subtype, mode)
        return 1
    }

    /**
     * Deletes all solve records for the given puzzle and category.
     * 
     * @param type    The name of the puzzle type.
     * @param subtype The name of the puzzle subtype.
     * @return The number of rows deleted.
     */
    fun deleteSubtype(type: String, subtype: String, mode: Int): Int {
        getSolveQueries().deleteSubtype(type, subtype, mode)
        return 1
    }

    /**
     * Renames a puzzle category (subtype) for the given puzzle type.
     * 
     * @param type       The name of the puzzle type.
     * @param oldSubtype The old name of the puzzle category.
     * @param newSubtype The new name of the puzzle category.
     * @return The number of rows updated.
     */
    fun renameSubtype(type: String, oldSubtype: String, newSubtype: String, mode: Int): Int {
        getSolveQueries().renameSubtype(newSubtype, type, oldSubtype, mode)
        return 1
    }

    /**
     * Returns a cursor that iterates over all solve records in the database.
     * 
     * @return A cursor over all solve records.
     */
    val allSolves: Cursor
        get() = readableDatabase.query(TABLE_TIMES, null, null, null, null, null, null)

    /**
     * Returns whether or not a solve record with the same solve details already exists in the
     * database.
     * 
     * @param solve The solve to be checked.
     * @return `true` if a record for the solve already exists; or `false` if not.
     */
    fun solveExists(solve: Solve): Boolean {
        return getSolveQueries().solveExists(
            solve.puzzle, solve.subtype, solve.time.toLong(), solve.date, solve.scramble, solve.mode
        ).executeAsOne() > 0
    }

    private fun createInitialAlgs(db: SQLiteDatabase) {
        // Now handled by DatabaseInitializer
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
        const val IDX_HISTORY: Int = 8
        const val IDX_MODE: Int = 9

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

        const val DIR_DESC: String = "DESC"
        const val DIR_ASC: String = "ASC"

        @JvmStatic
        fun modeToInt(mode: String?): Int {
            return if (mode == TimerFragment.TIMER_MODE_TRAINER) 1 else 0
        }
    }
}
