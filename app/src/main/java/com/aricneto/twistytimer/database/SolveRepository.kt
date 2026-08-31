package com.aricneto.twistytimer.database

import com.aricneto.twistytimer.items.Solve as DomainSolve
import com.aricneto.twistytimer.stats.ChartStatistics
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.PuzzleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

class SolveRepository(private val queries: SolveQueries) {

    suspend fun populateStatistics(
        puzzleType: String,
        puzzleSubtype: String,
        mode: Int,
        statistics: Statistics
    ) = withContext(Dispatchers.IO) {
        val isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly
        if (isStatisticsForCurrentSessionOnly) {
            val solves = queries.selectForStatistics(puzzleType, puzzleSubtype, mode).executeAsList()
            solves.forEach { sqSolve ->
                if ((sqSolve.penalty ?: 0).toInt() == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(true)
                } else {
                    statistics.addTime(sqSolve.time ?: 0, true)
                }
            }
        } else {
            val solves = queries.selectForAllStatistics(puzzleType, puzzleSubtype, mode).executeAsList()
            solves.forEach { sqSolve ->
                val isForCurrentSession = !(sqSolve.history ?: false)
                if ((sqSolve.penalty ?: 0).toInt() == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(isForCurrentSession)
                } else {
                    statistics.addTime(sqSolve.time ?: 0, isForCurrentSession)
                }
            }
        }
    }

    suspend fun populateChartStatistics(
        puzzleType: String,
        puzzleSubtype: String,
        mode: Int,
        statistics: ChartStatistics
    ) = withContext(Dispatchers.IO) {
        if (statistics.isForCurrentSessionOnly) {
            queries.selectForStatisticsWithDate(puzzleType, puzzleSubtype, mode).executeAsList().forEach { sqSolve ->
                if ((sqSolve.penalty ?: 0).toInt() == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(sqSolve.date)
                } else {
                    statistics.addTime(sqSolve.time ?: 0, sqSolve.date)
                }
            }
        } else {
            queries.selectForAllStatisticsWithDate(puzzleType, puzzleSubtype, mode).executeAsList().forEach { sqSolve ->
                if ((sqSolve.penalty ?: 0).toInt() == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(sqSolve.date)
                } else {
                    statistics.addTime(sqSolve.time ?: 0, sqSolve.date)
                }
            }
        }
    }

    fun getSolves(
        type: String,
        subtype: String,
        mode: Int,
        history: Boolean,
        search: String,
        orderByKey: String,
        orderByDir: String
    ): Flow<List<DomainSolve>> {
        val searchPattern = "%$search%"
        val key = if (orderByKey.contains("date")) "date" else "time"
        
        return queries.selectSolves(
            type = type,
            subtype = subtype,
            history = history,
            search = searchPattern,
            mode = mode,
            orderByKey = key,
            orderByDir = orderByDir
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { sqSolve ->
                    DomainSolve(
                        id = sqSolve._id,
                        time = (sqSolve.time ?: 0).toInt(),
                        puzzle = sqSolve.type ?: "",
                        subtype = sqSolve.subtype ?: "",
                        date = sqSolve.date,
                        scramble = sqSolve.scramble ?: "",
                        penalty = (sqSolve.penalty ?: 0).toInt(),
                        comment = sqSolve.comment ?: "",
                        history = sqSolve.history ?: false,
                        mode = sqSolve.mode
                    )
                }
            }
    }

    suspend fun getAllSolves() = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().map { sqSolve ->
            DomainSolve(
                id = sqSolve._id,
                time = (sqSolve.time ?: 0).toInt(),
                puzzle = sqSolve.type ?: "",
                subtype = sqSolve.subtype ?: "",
                date = sqSolve.date,
                scramble = sqSolve.scramble ?: "",
                penalty = (sqSolve.penalty ?: 0).toInt(),
                comment = sqSolve.comment ?: "",
                history = sqSolve.history ?: false,
                mode = sqSolve.mode
            )
        }
    }

    suspend fun getSolvesBySession(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.selectSolvesBySession(type, subtype, mode).executeAsList().map { sqSolve ->
            DomainSolve(
                id = sqSolve._id,
                time = (sqSolve.time ?: 0).toInt(),
                puzzle = sqSolve.type ?: "",
                subtype = sqSolve.subtype ?: "",
                date = sqSolve.date,
                scramble = sqSolve.scramble ?: "",
                penalty = (sqSolve.penalty ?: 0).toInt(),
                comment = sqSolve.comment ?: "",
                history = sqSolve.history ?: false,
                mode = sqSolve.mode
            )
        }
    }

    suspend fun insertSolve(
        type: String,
        subtype: String,
        time: Long,
        date: Long,
        scramble: String,
        penalty: Long,
        comment: String,
        history: Boolean,
        mode: Int
    ): Long = withContext(Dispatchers.IO) {
        queries.transactionWithResult {
            queries.insertSolve(type, subtype, time, date, scramble, penalty, comment, history, mode)
            queries.lastInsertId().executeAsOne()
        }
    }

    suspend fun updateSolve(
        id: Long,
        time: Long,
        date: Long,
        scramble: String,
        penalty: Long,
        comment: String,
        history: Boolean,
        mode: Int
    ) = withContext(Dispatchers.IO) {
        queries.updateSolve(time, date, scramble, penalty, comment, history, mode, id)
    }

    suspend fun deleteSolve(id: Long) = withContext(Dispatchers.IO) {
        queries.deleteSolve(id)
    }

    suspend fun deleteAllFromSession(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.deleteAllFromSession(type, subtype, mode)
    }

    suspend fun renameSubtype(newSubtype: String, type: String, oldSubtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.renameSubtype(newSubtype, type, oldSubtype, mode)
    }

    suspend fun moveAllSolvesToHistory(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.moveAllSolvesToHistory(type, subtype, mode)
    }

    suspend fun unarchiveSolves(type: String, subtype: String, mode: Int, limit: Long) = withContext(Dispatchers.IO) {
        queries.unarchiveSolves(type, subtype, mode, limit)
    }

    suspend fun getNumArchivedSolves(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.getNumArchivedSolves(type, subtype, mode).executeAsOne()
    }

    suspend fun getForStatistics(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.selectForStatistics(type, subtype, mode).executeAsList().map { sqSolve ->
            DomainSolve(
                id = 0,
                time = (sqSolve.time ?: 0).toInt(),
                puzzle = "",
                subtype = "",
                date = 0,
                scramble = "",
                penalty = (sqSolve.penalty ?: 0).toInt(),
                comment = "",
                history = false,
                mode = mode
            )
        }
    }

    suspend fun getForAllStatistics(type: String, subtype: String, mode: Int) = withContext(Dispatchers.IO) {
        queries.selectForAllStatistics(type, subtype, mode).executeAsList().map { sqSolve ->
            DomainSolve(
                id = 0,
                time = (sqSolve.time ?: 0).toInt(),
                puzzle = "",
                subtype = "",
                date = 0,
                scramble = "",
                penalty = (sqSolve.penalty ?: 0).toInt(),
                comment = "",
                history = sqSolve.history ?: false,
                mode = mode
            )
        }
    }

    suspend fun solveExists(
        type: String,
        subtype: String,
        time: Long,
        date: Long,
        scramble: String,
        mode: Int
    ) = withContext(Dispatchers.IO) {
        queries.solveExists(type, subtype, time, date, scramble, mode).executeAsOne() > 0
    }

    suspend fun migrateTrainingMode() = withContext(Dispatchers.IO) {
        queries.migrateTrainingMode()
    }
}
