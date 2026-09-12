package com.aricneto.twistytimer.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ImportExportManager(
    private val contentResolver: ContentResolver,
    private val solveRepository: SolveRepository
) {

    data class ImportResult(
        val successes: Int,
        val duplicates: Int,
        val parseErrors: Int
    )

    suspend fun exportSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String,
        mode: Int,
        onProgress: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val os = contentResolver.openOutputStream(uri) ?: return@withContext false
            val out = OutputStreamWriter(os)
            var exports = 0

            when (fileFormat) {
                ExportImportDialog.EXIM_FORMAT_BACKUP -> {
                    val csvHeader = "Puzzle;Category;Time(millis);Date(millis);Scramble;Penalty;Comment;Mode\n"
                    val solves = solveRepository.getAllSolves()
                    onProgress(0, solves.size)
                    out.write(csvHeader)

                    for (solve in solves) {
                        out.write(
                            ("\"" + solve.puzzle
                                    + "\";\"" + solve.subtype
                                    + "\";\"" + solve.time
                                    + "\";\"" + solve.date
                                    + "\";\"" + solve.scramble
                                    + "\";\"" + solve.penalty
                                    + "\";\"" + solve.comment
                                    + "\";\"" + solve.mode
                                    + "\"\n")
                        )
                        exports++
                        onProgress(exports, solves.size)
                    }
                }

                ExportImportDialog.EXIM_FORMAT_EXTERNAL -> {
                    val solves = solveRepository.getSolvesForExport(puzzleType, puzzleCategory, mode)
                    onProgress(0, solves.size)

                    for (solve in solves) {
                        var csvValues = ("\"" + convertTimeToString(
                            solve.time.toLong(),
                            PuzzleUtils.FORMAT_DEFAULT
                        )
                                + "\";\"" + solve.scramble
                                + "\";\"" + Instant.fromEpochMilliseconds(solve.date).toLocalDateTime(TimeZone.currentSystemDefault())
                                + "\"")

                        if (solve.penalty == PuzzleUtils.PENALTY_DNF) {
                            csvValues += ";\"DNF\""
                        }
                        csvValues += '\n'
                        out.write(csvValues)
                        exports++
                        onProgress(exports, solves.size)
                    }
                }
                else -> return@withContext false
            }
            out.close()
            true
        } catch (e: Exception) {
            Log.e("ImportExportManager", "Export error", e)
            false
        }
    }

    suspend fun importSolves(
        fileFormat: Int,
        uri: Uri,
        puzzleType: String,
        puzzleCategory: String,
        mode: Int,
        onProgress: (Int, Int) -> Unit
    ): ImportResult = withContext(Dispatchers.IO) {
        val solveList: MutableList<Solve> = ArrayList()
        var parseErrors = 0
        var successes = 0

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext ImportResult(0, 0, 0)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val parser = CSVParserBuilder()
                .withSeparator(';')
                .withQuoteChar('"')
                .withStrictQuotes(true)
                .build()
            val csvReader = CSVReaderBuilder(reader)
                .withCSVParser(parser)
                .build()

            if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                csvReader.readNext() // header
                while (true) {
                    val nextLine = csvReader.readNext() ?: break
                    try {
                        if (nextLine.size >= 7) {
                            solveList.add(
                                Solve(
                                    nextLine[2]?.toIntOrNull() ?: throw Exception("Invalid time"),
                                    nextLine[0] ?: "",
                                    nextLine[1] ?: "",
                                    nextLine[3]?.toLongOrNull() ?: 0L,
                                    nextLine[4] ?: "",
                                    nextLine[5]?.toIntOrNull() ?: 0,
                                    nextLine[6] ?: "",
                                    true,
                                    if (nextLine.size >= 8) nextLine[7]?.toIntOrNull() ?: 0 else 0
                                )
                            )
                        } else {
                            parseErrors++
                        }
                    } catch (_: Exception) {
                        parseErrors++
                    }
                }
            } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                val now = Clock.System.now().toEpochMilliseconds()
                while (true) {
                    val nextLine = csvReader.readNext() ?: break
                    if (nextLine.size <= 4) {
                        try {
                            val time = PuzzleUtils.parseTime(nextLine[0] ?: throw Exception("Missing time"))
                            var scramble = ""
                            var date = now
                            var penalty = PuzzleUtils.NO_PENALTY

                            if (nextLine.size >= 2) scramble = nextLine[1] ?: ""
                            if (nextLine.size >= 3) {
                                nextLine[2]?.let {
                                    try {
                                        date = LocalDateTime.parse(it)
                                            .toInstant(TimeZone.currentSystemDefault())
                                            .toEpochMilliseconds()
                                    } catch (_: Exception) {}
                                }
                            }
                            if (nextLine.size >= 4 && "DNF" == nextLine[3]) {
                                penalty = PuzzleUtils.PENALTY_DNF
                            }

                            solveList.add(
                                Solve(time, puzzleType, puzzleCategory, date, scramble, penalty, "", true, mode)
                            )
                        } catch (_: Exception) {
                            parseErrors++
                        }
                    } else {
                        parseErrors++
                    }
                }
            }

            successes = solveRepository.addSolves(
                solveList,
                fileFormat,
                object : SolveRepository.ProgressListener {
                    override fun onProgress(count: Int, total: Int) {
                        onProgress(count, total)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("ImportExportManager", "Import error", e)
        }

        ImportResult(successes, solveList.size - successes, parseErrors)
    }
}
